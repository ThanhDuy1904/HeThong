package fit.tedu.HeThong.service;

import fit.tedu.HeThong.dto.request.StudentRequest;
import fit.tedu.HeThong.dto.response.StudentResponse;
import fit.tedu.HeThong.entity.*;
import fit.tedu.HeThong.repository.*;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;
import java.util.LinkedHashSet;
import java.util.Objects;
import java.util.Optional;
import java.text.Collator;
import java.util.Comparator;
import java.util.Locale;
import java.util.stream.Collectors;
import org.springframework.web.multipart.MultipartFile;
import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xwpf.usermodel.XWPFDocument;

@Service
@RequiredArgsConstructor
public class StudentService {

    private final StudentRepository studentRepository;
    private final ClassRoomRepository classRoomRepository;
    private final TeacherRepository teacherRepository;
    private final UserRepository userRepository;
    private final RoleRepository roleRepository;
    private final PasswordEncoder passwordEncoder;
    private final TuitionPaymentService tuitionPaymentService;
    private final StudentClassTuitionRepository studentClassTuitionRepository;

    public List<StudentResponse> getAll() {
        return studentRepository.findAll().stream().map(this::toResponse)
                .sorted(lastNameComparator()).collect(Collectors.toList());
    }

    public StudentResponse getById(Long id) {
        return toResponse(studentRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy học sinh ID: " + id)));
    }

    public StudentResponse getByIdForTeacher(Long id, String username) {
        Student student = studentRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy học sinh ID: " + id));
        Long teacherId = teacherRepository.findByUsername(username)
                .map(Teacher::getId)
                .orElseThrow(() -> new org.springframework.security.access.AccessDeniedException(
                        "Tài khoản không phải giáo viên"));
        boolean assigned = student.getClasses().stream()
                .anyMatch(cls -> cls.getTeacher() != null && cls.getTeacher().getId().equals(teacherId));
        if (!assigned && (student.getClassRoom() == null || student.getClassRoom().getTeacher() == null
                || !student.getClassRoom().getTeacher().getId().equals(teacherId))) {
            throw new org.springframework.security.access.AccessDeniedException(
                    "Giáo viên không phụ trách học sinh này");
        }
        return toResponse(student);
    }

    public List<StudentResponse> search(String keyword) {
        return studentRepository.searchByKeyword(keyword).stream().map(this::toResponse)
                .sorted(lastNameComparator()).collect(Collectors.toList());
    }

    public List<StudentResponse> getByClass(Long classId) {
        ClassRoom selectedClass = classRoomRepository.findById(classId)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy lớp học"));
        return studentRepository.findByAnyClassId(classId).stream()
                .map(student -> toResponse(student, selectedClass,
                        studentClassTuitionRepository.findByStudentIdAndClassRoomId(student.getId(), classId)
                                .map(StudentClassTuition::getPaidAmount)
                                .orElse(student.getClassRoom() != null && student.getClassRoom().getId().equals(classId)
                                        ? Optional.ofNullable(student.getTuitionPaidAmount()).orElse(BigDecimal.ZERO)
                                        : BigDecimal.ZERO)))
                .collect(Collectors.toList());
    }

    public List<StudentResponse> getByClassForTeacher(Long classId, String username) {
        boolean assigned = teacherRepository.findByUsername(username)
                .map(teacher -> classRoomRepository.findById(classId)
                        .map(classRoom -> classRoom.getTeacher() != null
                                && classRoom.getTeacher().getId().equals(teacher.getId()))
                        .orElse(false))
                .orElse(false);
        if (!assigned) {
            throw new org.springframework.security.access.AccessDeniedException("Giáo viên không phụ trách lớp này");
        }
        return getByClass(classId);
    }

    @Transactional
    public StudentResponse create(StudentRequest req) {
        Student student = new Student();
        student.setFullName(req.getFullName());
        student.setDob(req.getDob());
        student.setGender(req.getGender());
        student.setAddress(req.getAddress());
        student.setPhone(req.getPhone());
        student.setParentName(req.getParentName());
        student.setParentPhone(req.getParentPhone());
        student.setStatus(req.getStatus() != null ? req.getStatus() : "ACTIVE");
        student.setTuitionPaidAmount(req.getTuitionPaidAmount() != null ? req.getTuitionPaidAmount() : BigDecimal.ZERO);
        student.setTuitionPaidFull(Boolean.TRUE.equals(req.getTuitionPaidFull()));

        if (req.getClassId() != null) {
            classRoomRepository.findById(req.getClassId()).ifPresent(cls -> {
                student.setClassRoom(cls);
                student.getClasses().add(cls);
            });
        }

        assignClasses(student, req);
        normalizeTuition(student);

        // Create linked user account if provided
        if (req.getUsername() != null && !req.getUsername().isBlank()) {
            if (userRepository.existsByUsername(req.getUsername())) {
                throw new RuntimeException("Tên đăng nhập đã tồn tại!");
            }
            Role role = roleRepository.findByName("STUDENT").orElseThrow();
            User user = User.builder()
                    .username(req.getUsername())
                    .password(passwordEncoder.encode(req.getPassword() != null ? req.getPassword() : "student123"))
                    .email(req.getEmail())
                    .fullName(req.getFullName())
                    .role(role)
                    .enabled(true)
                    .build();
            userRepository.save(user);
            student.setUser(user);
        }

        Student saved = studentRepository.save(student);
        initializeClassTuition(saved);
        syncLegacyTuition(saved);
        if (saved.getTuitionPaidAmount() != null && saved.getTuitionPaidAmount().compareTo(BigDecimal.ZERO) > 0) {
            tuitionPaymentService.recordPaymentChange(saved.getId(), saved.getTuitionPaidAmount(),
                toResponse(saved).getTuitionRemaining(), "Khởi tạo học phí", null);
        }
        return toResponse(saved);
    }

    @Transactional
    public int importFile(MultipartFile file, Long defaultClassId) {
        if (file == null || file.isEmpty()) {
            throw new IllegalArgumentException("Vui lòng chọn file danh sách học sinh");
        }
        String name = file.getOriginalFilename() == null ? "" : file.getOriginalFilename().toLowerCase();
        try {
            List<String[]> rows = new java.util.ArrayList<>();
            if (name.endsWith(".xlsx") || name.endsWith(".xls")) {
                try (Workbook workbook = WorkbookFactory.create(file.getInputStream())) {
                    Sheet sheet = workbook.getSheetAt(0);
                    for (Row row : sheet) {
                        String[] values = new String[5];
                        for (int i = 0; i < values.length; i++) {
                            Cell cell = row.getCell(i);
                            values[i] = cell == null ? "" : new DataFormatter().formatCellValue(cell).trim();
                        }
                        if (!values[0].isBlank() && !values[0].equalsIgnoreCase("họ tên")
                                && !values[0].equalsIgnoreCase("ho ten")) rows.add(values);
                    }
                }
            } else if (name.endsWith(".docx")) {
                try (XWPFDocument document = new XWPFDocument(file.getInputStream())) {
                    document.getTables().forEach(table -> table.getRows().forEach(row -> {
                        List<String> cells = row.getTableCells().stream().map(c -> c.getText().trim()).toList();
                        if (!cells.isEmpty() && !cells.get(0).equalsIgnoreCase("họ tên")
                                && !cells.get(0).equalsIgnoreCase("ho ten")) {
                            rows.add(java.util.stream.Stream.concat(cells.stream(), java.util.stream.Stream.generate(() -> ""))
                                    .limit(5).toArray(String[]::new));
                        }
                    }));
                }
            } else {
                try (BufferedReader reader = new BufferedReader(new InputStreamReader(
                        file.getInputStream(), StandardCharsets.UTF_8))) {
                    String line;
                    boolean first = true;
                    while ((line = reader.readLine()) != null) {
                        if (first && line.toLowerCase().contains("họ")) { first = false; continue; }
                        first = false;
                        String[] values = line.split(",", -1);
                        if (values.length > 0 && !values[0].trim().isBlank()) {
                            rows.add(java.util.Arrays.copyOf(values, 5));
                        }
                    }
                }
            }
            int imported = 0;
            for (String[] row : rows) {
                StudentRequest request = new StudentRequest();
                request.setFullName(row[0].trim());
                request.setPhone(row[1].trim());
                request.setParentPhone(row[2].trim());
                request.setParentName(row[3].trim());
                Long classId = defaultClassId;
                if (classId == null && row[4] != null && !row[4].isBlank()) {
                    classId = classRoomRepository.findAll().stream()
                            .filter(c -> c.getClassName().equalsIgnoreCase(row[4].trim()))
                            .map(ClassRoom::getId).findFirst().orElseGet(() -> {
                                ClassRoom newClass = ClassRoom.builder().className(row[4].trim()).build();
                                return classRoomRepository.save(newClass).getId();
                            });
                }
                request.setClassId(classId);
                create(request);
                imported++;
            }
            return imported;
        } catch (Exception e) {
            throw new IllegalArgumentException("Không thể đọc file danh sách: " + e.getMessage(), e);
        }
    }

    @Transactional
    public StudentResponse update(Long id, StudentRequest req) {
        Student student = studentRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy học sinh!"));
        ensureEditable(student);

        Long selectedClassId = req.getClassId() != null ? req.getClassId()
                : student.getClassRoom() != null ? student.getClassRoom().getId() : null;
        BigDecimal oldPaid = selectedClassId == null ? BigDecimal.ZERO
                : studentClassTuitionRepository.findByStudentIdAndClassRoomId(id, selectedClassId)
                        .map(StudentClassTuition::getPaidAmount).orElse(BigDecimal.ZERO);
        student.setFullName(req.getFullName());
        student.setDob(req.getDob());
        student.setGender(req.getGender());
        student.setAddress(req.getAddress());
        student.setPhone(req.getPhone());
        student.setParentName(req.getParentName());
        student.setParentPhone(req.getParentPhone());
        if (req.getStatus() != null) student.setStatus(req.getStatus());
        if (req.getTuitionPaidAmount() != null) student.setTuitionPaidAmount(req.getTuitionPaidAmount());
        if (req.getTuitionPaidFull() != null) student.setTuitionPaidFull(req.getTuitionPaidFull());
        if (req.getClassId() != null) {
            classRoomRepository.findById(req.getClassId()).ifPresent(cls -> student.setClassRoom(cls));
        }
        assignClasses(student, req);
        normalizeTuition(student);

        // Update user account password if provided
        if (student.getUser() != null && req.getPassword() != null && !req.getPassword().isBlank()) {
            User user = student.getUser();
            user.setPassword(passwordEncoder.encode(req.getPassword()));
            if (req.getEmail() != null) user.setEmail(req.getEmail());
            userRepository.save(user);
        }

        Student saved = studentRepository.save(student);
        initializeClassTuition(saved);
        BigDecimal newPaid = saved.getTuitionPaidAmount() != null ? saved.getTuitionPaidAmount() : BigDecimal.ZERO;
        if (selectedClassId != null && req.getTuitionPaidAmount() != null) {
            ClassRoom selectedClass = classRoomRepository.findById(selectedClassId)
                    .orElseThrow(() -> new RuntimeException("Không tìm thấy lớp học"));
            StudentClassTuition classTuition = studentClassTuitionRepository
                    .findByStudentIdAndClassRoomId(id, selectedClassId)
                    .orElseGet(() -> StudentClassTuition.builder()
                            .student(saved).classRoom(selectedClass).paidAmount(BigDecimal.ZERO).build());
            newPaid = req.getTuitionPaidAmount();
            BigDecimal fee = selectedClass.getTuitionFee() == null ? BigDecimal.ZERO : selectedClass.getTuitionFee();
            if (newPaid.compareTo(BigDecimal.ZERO) < 0 || newPaid.compareTo(fee) > 0) {
                throw new RuntimeException("Số tiền đã đóng không hợp lệ");
            }
            classTuition.setPaidAmount(newPaid);
            studentClassTuitionRepository.save(classTuition);
            if (saved.getClassRoom() != null && saved.getClassRoom().getId().equals(selectedClassId)) {
                saved.setTuitionPaidAmount(newPaid);
                saved.setTuitionPaidFull(newPaid.compareTo(fee) >= 0);
                studentRepository.save(saved);
            }
        }
        BigDecimal delta = newPaid.subtract(oldPaid);
        if (delta.compareTo(BigDecimal.ZERO) != 0) {
            tuitionPaymentService.recordPaymentChange(saved.getId(), delta, toResponse(saved).getTuitionRemaining(),
                "Cập nhật học phí", null);
        }
        return toResponse(saved);
    }

    @Transactional
    public void delete(Long id) {
        Student student = studentRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy học sinh!"));
        ensureEditable(student);
        
        // Delete linked user account if exists
        User user = student.getUser();
        studentRepository.deleteById(id);
        
        if (user != null) {
            userRepository.delete(user);
        }
    }

    @Transactional
    public StudentResponse removeFromClass(Long id) {
        Student student = studentRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy học sinh!"));
        ensureEditable(student);
        student.setClassRoom(null);
        student.getClasses().clear();
        return toResponse(studentRepository.save(student));
    }

    public StudentResponse getByUserId(Long userId) {
        Student student = studentRepository.findByUserId(userId)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy thông tin học sinh!"));
        return toResponse(student);
    }

    public StudentResponse getByUsername(String username) {
        Student student = studentRepository.findByUsername(username)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy thông tin học sinh!"));
        return toResponse(student);
    }

    public long countAll() { return studentRepository.count(); }
    public long countByStatus(String status) { return studentRepository.countByStatus(status); }

    private void normalizeTuition(Student student) {
        BigDecimal paid = student.getTuitionPaidAmount() != null ? student.getTuitionPaidAmount() : BigDecimal.ZERO;
        if (paid.compareTo(BigDecimal.ZERO) < 0) {
            throw new RuntimeException("Số tiền đã đóng không hợp lệ");
        }

        BigDecimal classFee = student.getClassRoom() != null && student.getClassRoom().getTuitionFee() != null
                ? student.getClassRoom().getTuitionFee()
                : BigDecimal.ZERO;

        if (classFee.compareTo(BigDecimal.ZERO) <= 0) {
            student.setTuitionPaidAmount(paid);
            student.setTuitionPaidFull(student.isTuitionPaidFull() && paid.compareTo(BigDecimal.ZERO) > 0);
            return;
        }

        if (student.isTuitionPaidFull()) {
            student.setTuitionPaidAmount(classFee);
            return;
        }

        if (paid.compareTo(classFee) >= 0 && classFee.compareTo(BigDecimal.ZERO) > 0) {
            student.setTuitionPaidFull(true);
            student.setTuitionPaidAmount(classFee);
            return;
        }

        student.setTuitionPaidAmount(paid);
    }

    private void ensureEditable(Student student) {
        if (student.getClasses().stream().anyMatch(ClassRoom::isArchived)
                || (student.getClassRoom() != null && student.getClassRoom().isArchived())) {
            throw new IllegalStateException("Học sinh thuộc lớp đã lưu trữ, chỉ được xem");
        }
    }

    private StudentResponse toResponse(Student s) {
        BigDecimal classFee = s.getClassRoom() != null && s.getClassRoom().getTuitionFee() != null
                ? s.getClassRoom().getTuitionFee()
                : BigDecimal.ZERO;
        BigDecimal paid = s.getTuitionPaidAmount() != null ? s.getTuitionPaidAmount() : BigDecimal.ZERO;
        BigDecimal remaining = classFee.subtract(paid);
        if (remaining.compareTo(BigDecimal.ZERO) < 0) {
            remaining = BigDecimal.ZERO;
        }

        return StudentResponse.builder()
                .id(s.getId())
                .fullName(s.getFullName())
                .dob(s.getDob())
                .gender(s.getGender())
                .address(s.getAddress())
                .phone(s.getPhone())
                .parentName(s.getParentName())
                .parentPhone(s.getParentPhone())
                .status(s.getStatus())
                .classId(s.getClassRoom() != null ? s.getClassRoom().getId() : null)
                .className(s.getClassRoom() != null ? s.getClassRoom().getClassName() : null)
                .classIds(s.getClasses().stream().map(ClassRoom::getId).filter(Objects::nonNull).toList())
                .classNames(s.getClasses().stream().map(ClassRoom::getClassName).filter(Objects::nonNull).toList())
                .classTuitionFee(s.getClassRoom() != null ? s.getClassRoom().getTuitionFee() : null)
                .tuitionPaidAmount(paid)
                .tuitionPaidFull(s.isTuitionPaidFull())
                .tuitionRemaining(remaining)
                .userId(s.getUser() != null ? s.getUser().getId() : null)
                .username(s.getUser() != null ? s.getUser().getUsername() : null)
                .createdAt(s.getCreatedAt())
                .build();
    }

    private Comparator<StudentResponse> lastNameComparator() {
        Collator collator = Collator.getInstance(new Locale("vi", "VN"));
        return Comparator.comparing((StudentResponse s) -> {
            String name = s.getFullName() == null ? "" : s.getFullName().trim();
            return name.isBlank() ? "" : name.substring(name.lastIndexOf(' ') + 1);
        }, collator).thenComparing(StudentResponse::getFullName, Comparator.nullsLast(collator));
    }

    private StudentResponse toResponse(Student s, ClassRoom selectedClass) {
        return toResponse(s, selectedClass,
                studentClassTuitionRepository.findByStudentIdAndClassRoomId(s.getId(), selectedClass.getId())
                        .map(StudentClassTuition::getPaidAmount).orElse(BigDecimal.ZERO));
    }

    private StudentResponse toResponse(Student s, ClassRoom selectedClass, BigDecimal paid) {
        StudentResponse response = toResponse(s);
        BigDecimal fee = selectedClass.getTuitionFee() == null ? BigDecimal.ZERO : selectedClass.getTuitionFee();
        BigDecimal remaining = fee.subtract(paid).max(BigDecimal.ZERO);
        response.setClassId(selectedClass.getId());
        response.setClassName(selectedClass.getClassName());
        response.setClassTuitionFee(fee);
        response.setTuitionRemaining(remaining);
        response.setTuitionPaidFull(remaining.compareTo(BigDecimal.ZERO) == 0);
        return response;
    }

    private void initializeClassTuition(Student student) {
        for (ClassRoom classRoom : student.getClasses()) {
            studentClassTuitionRepository.findByStudentIdAndClassRoomId(student.getId(), classRoom.getId())
                    .orElseGet(() -> studentClassTuitionRepository.save(StudentClassTuition.builder()
                            .student(student).classRoom(classRoom)
                            .paidAmount(student.getClassRoom() != null && student.getClassRoom().getId().equals(classRoom.getId())
                                    ? Optional.ofNullable(student.getTuitionPaidAmount()).orElse(BigDecimal.ZERO)
                                    : BigDecimal.ZERO)
                            .build()));
        }
    }

    private void syncLegacyTuition(Student student) {
        if (student.getClassRoom() == null) return;
        studentClassTuitionRepository.findByStudentIdAndClassRoomId(student.getId(), student.getClassRoom().getId())
                .ifPresent(tuition -> {
                    tuition.setPaidAmount(Optional.ofNullable(student.getTuitionPaidAmount()).orElse(BigDecimal.ZERO));
                    studentClassTuitionRepository.save(tuition);
                });
    }

    private void assignClasses(Student student, StudentRequest req) {
        boolean classSelectionProvided = req.getClassIds() != null || req.getClassId() != null;
        List<Long> ids = req.getClassIds() == null ? List.of() : req.getClassIds().stream()
                .filter(Objects::nonNull).distinct().toList();
        if (req.getClassId() != null && !ids.contains(req.getClassId())) {
            ids = new java.util.ArrayList<>(ids);
            ids.add(req.getClassId());
        }
        if (classSelectionProvided) {
            List<ClassRoom> classes = classRoomRepository.findAllById(ids);
            student.setClasses(new LinkedHashSet<>(classes));
            if (classes.isEmpty()) {
                student.setClassRoom(null);
            } else if (student.getClassRoom() == null || !ids.contains(student.getClassRoom().getId())) {
                student.setClassRoom(classes.get(0));
            }
        }
    }
}
