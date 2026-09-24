package fit.tedu.HeThong.service;

import fit.tedu.HeThong.dto.response.TuitionPaymentResponse;
import fit.tedu.HeThong.dto.response.StudentResponse;
import fit.tedu.HeThong.dto.response.ArchiveFileResponse;
import fit.tedu.HeThong.entity.Student;
import fit.tedu.HeThong.entity.TuitionPayment;
import fit.tedu.HeThong.entity.User;
import fit.tedu.HeThong.entity.ClassRoom;
import fit.tedu.HeThong.entity.StudentClassTuition;
import fit.tedu.HeThong.repository.StudentRepository;
import fit.tedu.HeThong.repository.TuitionPaymentRepository;
import fit.tedu.HeThong.repository.UserRepository;
import fit.tedu.HeThong.repository.StudentClassTuitionRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;
import java.util.stream.Collectors;
import java.time.LocalDateTime;
import java.nio.charset.StandardCharsets;
import java.util.Locale;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class TuitionPaymentService {

    private final TuitionPaymentRepository tuitionPaymentRepository;
    private final StudentRepository studentRepository;
    private final UserRepository userRepository;
    private final ArchiveService archiveService;
    private final StudentClassTuitionRepository studentClassTuitionRepository;

    @Transactional
    public void recordPaymentChange(Long studentId, BigDecimal amountDelta, BigDecimal balanceAfter, String note, String username) {
        if (amountDelta == null || amountDelta.compareTo(BigDecimal.ZERO) == 0) {
            return;
        }

        Student student = studentRepository.findById(studentId)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy học sinh!"));
        User creator = null;
        if (username != null && !username.isBlank()) {
            creator = userRepository.findByUsername(username).orElse(null);
        }

        TuitionPayment payment = TuitionPayment.builder()
                .student(student)
                .createdBy(creator)
                .amount(amountDelta)
                .balanceAfter(balanceAfter)
                .note(note)
                .build();
        tuitionPaymentRepository.save(payment);
    }

    @Transactional
    public StudentResponse collectPayment(Long studentId, Long classId, BigDecimal amount, String note, String username) {
        if (amount == null || amount.compareTo(BigDecimal.ZERO) <= 0) {
            throw new RuntimeException("Số tiền thu phải lớn hơn 0");
        }

        Student student = studentRepository.findByIdForUpdate(studentId)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy học sinh!"));

        ClassRoom classRoom = classId == null ? student.getClassRoom() : student.getClasses().stream()
                .filter(item -> item.getId().equals(classId)).findFirst().orElseThrow(() -> new RuntimeException("Học sinh không thuộc lớp này"));
        BigDecimal classFee = classRoom != null && classRoom.getTuitionFee() != null
                ? classRoom.getTuitionFee()
                : BigDecimal.ZERO;
        if (classFee.compareTo(BigDecimal.ZERO) <= 0) {
            throw new RuntimeException("Lớp học chưa thiết lập học phí");
        }

        StudentClassTuition tuition = studentClassTuitionRepository.findByStudentIdAndClassRoomId(studentId, classRoom.getId())
                .orElseGet(() -> studentClassTuitionRepository.save(StudentClassTuition.builder()
                        .student(student).classRoom(classRoom).paidAmount(BigDecimal.ZERO).build()));
        BigDecimal currentPaid = tuition.getPaidAmount() == null ? BigDecimal.ZERO : tuition.getPaidAmount();
        BigDecimal remaining = classFee.subtract(currentPaid);
        if (remaining.compareTo(BigDecimal.ZERO) < 0) {
            remaining = BigDecimal.ZERO;
        }
        if (amount.compareTo(remaining) > 0) {
            throw new RuntimeException("Số tiền thu vượt quá số còn lại");
        }

        BigDecimal newPaid = currentPaid.add(amount);
        tuition.setPaidAmount(newPaid);
        studentClassTuitionRepository.save(tuition);
        if (student.getClassRoom() != null && student.getClassRoom().getId().equals(classRoom.getId())) {
            student.setTuitionPaidAmount(newPaid);
            student.setTuitionPaidFull(newPaid.compareTo(classFee) >= 0);
        }
        Student saved = studentRepository.save(student);

        recordPaymentChange(saved.getId(), amount, classFee.subtract(newPaid).max(BigDecimal.ZERO),
                note != null && !note.isBlank() ? note : "Thu học phí", username);
        return toStudentResponse(saved, classRoom, newPaid);
    }

    public List<TuitionPaymentResponse> getByStudentId(Long studentId) {
        return tuitionPaymentRepository.findByStudentIdOrderByCreatedAtDesc(studentId).stream().map(this::toResponse).collect(Collectors.toList());
    }

    public List<TuitionPaymentResponse> getByClassId(Long classId) {
        return tuitionPaymentRepository.findByStudentClassRoomIdOrderByCreatedAtDesc(classId).stream().map(this::toResponse).collect(Collectors.toList());
    }

    @Transactional
    public ArchiveFileResponse closeClass(Long classId, String requestedFileName, String username) {
        List<Student> students = studentRepository.findByAnyClassId(classId);
        if (students.isEmpty()) throw new RuntimeException("Lớp chưa có học sinh");
        String className = students.stream().flatMap(s -> s.getClasses().stream())
                .filter(c -> c.getId().equals(classId)).map(ClassRoom::getClassName).findFirst()
                .orElseGet(() -> students.get(0).getClassRoom().getClassName());
        ClassRoom selectedClass = students.stream().flatMap(s -> s.getClasses().stream())
                .filter(c -> c.getId().equals(classId)).findFirst()
                .orElse(students.get(0).getClassRoom());
        BigDecimal fee = selectedClass == null || selectedClass.getTuitionFee() == null
                ? BigDecimal.ZERO : selectedClass.getTuitionFee();
        for (Student student : students) {
            BigDecimal paid = studentClassTuitionRepository.findByStudentIdAndClassRoomId(student.getId(), classId)
                    .map(StudentClassTuition::getPaidAmount)
                    .orElse(student.getClassRoom() != null && student.getClassRoom().getId().equals(classId)
                            ? Optional.ofNullable(student.getTuitionPaidAmount()).orElse(BigDecimal.ZERO)
                            : BigDecimal.ZERO);
            if (fee.subtract(paid).compareTo(BigDecimal.ZERO) > 0) {
                throw new RuntimeException("Chưa thể lưu: vẫn còn học sinh chưa đóng đủ học phí");
            }
        }
        String fileName = requestedFileName.replaceAll("[^a-zA-Z0-9_-]", "_").trim();
        if (fileName.isBlank()) throw new RuntimeException("Tên file không hợp lệ");
        if (!fileName.toLowerCase(Locale.ROOT).endsWith(".pdf")) fileName += ".pdf";
        byte[] pdf = buildReceiptPdf(className, fee, students);
        ArchiveFileResponse archived = archiveService.saveGenerated(fileName, pdf, "application/pdf", username);
        students.forEach(student -> {
            studentClassTuitionRepository.findByStudentIdAndClassRoomId(student.getId(), classId)
                    .ifPresent(tuition -> {
                        tuition.setPaidAmount(BigDecimal.ZERO);
                        studentClassTuitionRepository.save(tuition);
                    });
            if (student.getClassRoom() != null && student.getClassRoom().getId().equals(classId)) {
                student.setTuitionPaidAmount(BigDecimal.ZERO);
                student.setTuitionPaidFull(false);
            }
        });
        studentRepository.saveAll(students);
        return archived;
    }

    private byte[] buildReceiptPdf(String className, BigDecimal fee, List<Student> students) {
        StringBuilder text = new StringBuilder("PHIEU CHOT HOC PHI\n");
        text.append("Lop: ").append(className).append("\n");
        text.append("Hoc phi moi hoc sinh: ").append(fee).append(" VND\n");
        text.append("Thoi gian: ").append(LocalDateTime.now()).append("\n\n");
        for (Student student : students) text.append(student.getFullName()).append(" - DA DONG DU\n");
        String escaped = text.toString().replace("\\", "\\\\").replace("(", "\\(").replace(")", "\\)")
                .replace("\n", ") Tj 0 -16 Td (");
        String body = "BT /F1 11 Tf 50 780 Td (" + escaped + ") Tj ET";
        String[] objects = {"<< /Type /Catalog /Pages 2 0 R >>", "<< /Type /Pages /Kids [3 0 R] /Count 1 >>",
                "<< /Type /Page /Parent 2 0 R /MediaBox [0 0 595 842] /Resources << /Font << /F1 5 0 R >> >> /Contents 4 0 R >>",
                "<< /Length " + body.getBytes(StandardCharsets.US_ASCII).length + " >>\nstream\n" + body + "\nendstream",
                "<< /Type /Font /Subtype /Type1 /BaseFont /Helvetica >>"};
        StringBuilder pdf = new StringBuilder("%PDF-1.4\n");
        int[] offsets = new int[objects.length + 1];
        for (int i = 0; i < objects.length; i++) { offsets[i + 1] = pdf.length(); pdf.append(i + 1).append(" 0 obj\n").append(objects[i]).append("\nendobj\n"); }
        int xref = pdf.length();
        pdf.append("xref\n0 ").append(objects.length + 1).append("\n0000000000 65535 f \n");
        for (int i = 1; i < offsets.length; i++) pdf.append(String.format(Locale.ROOT, "%010d 00000 n \n", offsets[i]));
        pdf.append("trailer\n<< /Size ").append(offsets.length).append(" /Root 1 0 R >>\nstartxref\n").append(xref).append("\n%%EOF");
        return pdf.toString().getBytes(StandardCharsets.US_ASCII);
    }

    private TuitionPaymentResponse toResponse(TuitionPayment payment) {
        return TuitionPaymentResponse.builder()
                .id(payment.getId())
                .studentId(payment.getStudent() != null ? payment.getStudent().getId() : null)
                .studentName(payment.getStudent() != null ? payment.getStudent().getFullName() : null)
                .className(payment.getStudent() != null && payment.getStudent().getClassRoom() != null ? payment.getStudent().getClassRoom().getClassName() : null)
                .amount(payment.getAmount())
                .balanceAfter(payment.getBalanceAfter())
                .note(payment.getNote())
                .createdById(payment.getCreatedBy() != null ? payment.getCreatedBy().getId() : null)
                .createdByName(payment.getCreatedBy() != null ? payment.getCreatedBy().getFullName() : null)
                .createdAt(payment.getCreatedAt())
                .build();
    }

    private StudentResponse toStudentResponse(Student s) {
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

    private StudentResponse toStudentResponse(Student s, ClassRoom classRoom, BigDecimal paid) {
        BigDecimal fee = classRoom.getTuitionFee() == null ? BigDecimal.ZERO : classRoom.getTuitionFee();
        BigDecimal remaining = fee.subtract(paid).max(BigDecimal.ZERO);
        StudentResponse response = toStudentResponse(s);
        response.setClassId(classRoom.getId());
        response.setClassName(classRoom.getClassName());
        response.setClassTuitionFee(fee);
        response.setTuitionPaidAmount(paid);
        response.setTuitionRemaining(remaining);
        response.setTuitionPaidFull(remaining.compareTo(BigDecimal.ZERO) == 0);
        return response;
    }

    private BigDecimal toRemaining(Student s) {
        BigDecimal classFee = s.getClassRoom() != null && s.getClassRoom().getTuitionFee() != null
                ? s.getClassRoom().getTuitionFee()
                : BigDecimal.ZERO;
        BigDecimal paid = s.getTuitionPaidAmount() != null ? s.getTuitionPaidAmount() : BigDecimal.ZERO;
        BigDecimal remaining = classFee.subtract(paid);
        return remaining.compareTo(BigDecimal.ZERO) < 0 ? BigDecimal.ZERO : remaining;
    }
}