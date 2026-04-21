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
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class StudentService {

    private final StudentRepository studentRepository;
    private final ClassRoomRepository classRoomRepository;
    private final UserRepository userRepository;
    private final RoleRepository roleRepository;
    private final PasswordEncoder passwordEncoder;

    public List<StudentResponse> getAll() {
        return studentRepository.findAll().stream().map(this::toResponse).collect(Collectors.toList());
    }

    public StudentResponse getById(Long id) {
        return toResponse(studentRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy học sinh ID: " + id)));
    }

    public List<StudentResponse> search(String keyword) {
        return studentRepository.searchByKeyword(keyword).stream().map(this::toResponse).collect(Collectors.toList());
    }

    public List<StudentResponse> getByClass(Long classId) {
        return studentRepository.findByClassRoomId(classId).stream().map(this::toResponse).collect(Collectors.toList());
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
            classRoomRepository.findById(req.getClassId()).ifPresent(student::setClassRoom);
        }
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

        return toResponse(studentRepository.save(student));
    }

    @Transactional
    public StudentResponse update(Long id, StudentRequest req) {
        Student student = studentRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy học sinh!"));

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
            classRoomRepository.findById(req.getClassId()).ifPresent(student::setClassRoom);
        }
        normalizeTuition(student);

        // Update user account password if provided
        if (student.getUser() != null && req.getPassword() != null && !req.getPassword().isBlank()) {
            User user = student.getUser();
            user.setPassword(passwordEncoder.encode(req.getPassword()));
            if (req.getEmail() != null) user.setEmail(req.getEmail());
            userRepository.save(user);
        }

        return toResponse(studentRepository.save(student));
    }

    @Transactional
    public void delete(Long id) {
        Student student = studentRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy học sinh!"));
        
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
        student.setClassRoom(null);
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
                .classTuitionFee(s.getClassRoom() != null ? s.getClassRoom().getTuitionFee() : null)
                .tuitionPaidAmount(paid)
                .tuitionPaidFull(s.isTuitionPaidFull())
                .tuitionRemaining(remaining)
                .userId(s.getUser() != null ? s.getUser().getId() : null)
                .username(s.getUser() != null ? s.getUser().getUsername() : null)
                .createdAt(s.getCreatedAt())
                .build();
    }
}
