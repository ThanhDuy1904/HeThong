package fit.tedu.HeThong.service;

import fit.tedu.HeThong.dto.response.TuitionPaymentResponse;
import fit.tedu.HeThong.dto.response.StudentResponse;
import fit.tedu.HeThong.entity.Student;
import fit.tedu.HeThong.entity.TuitionPayment;
import fit.tedu.HeThong.entity.User;
import fit.tedu.HeThong.repository.StudentRepository;
import fit.tedu.HeThong.repository.TuitionPaymentRepository;
import fit.tedu.HeThong.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class TuitionPaymentService {

    private final TuitionPaymentRepository tuitionPaymentRepository;
    private final StudentRepository studentRepository;
    private final UserRepository userRepository;

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
    public StudentResponse collectPayment(Long studentId, BigDecimal amount, String note, String username) {
        if (amount == null || amount.compareTo(BigDecimal.ZERO) <= 0) {
            throw new RuntimeException("Số tiền thu phải lớn hơn 0");
        }

        Student student = studentRepository.findByIdForUpdate(studentId)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy học sinh!"));

        BigDecimal classFee = student.getClassRoom() != null && student.getClassRoom().getTuitionFee() != null
                ? student.getClassRoom().getTuitionFee()
                : BigDecimal.ZERO;
        if (classFee.compareTo(BigDecimal.ZERO) <= 0) {
            throw new RuntimeException("Lớp học chưa thiết lập học phí");
        }

        BigDecimal currentPaid = student.getTuitionPaidAmount() != null ? student.getTuitionPaidAmount() : BigDecimal.ZERO;
        BigDecimal remaining = classFee.subtract(currentPaid);
        if (remaining.compareTo(BigDecimal.ZERO) < 0) {
            remaining = BigDecimal.ZERO;
        }
        if (amount.compareTo(remaining) > 0) {
            throw new RuntimeException("Số tiền thu vượt quá số còn lại");
        }

        BigDecimal newPaid = currentPaid.add(amount);
        student.setTuitionPaidAmount(newPaid);
        student.setTuitionPaidFull(newPaid.compareTo(classFee) >= 0);
        Student saved = studentRepository.save(student);

        recordPaymentChange(saved.getId(), amount, toRemaining(saved), note != null && !note.isBlank() ? note : "Thu học phí", username);
        return toStudentResponse(saved);
    }

    public List<TuitionPaymentResponse> getByStudentId(Long studentId) {
        return tuitionPaymentRepository.findByStudentIdOrderByCreatedAtDesc(studentId).stream().map(this::toResponse).collect(Collectors.toList());
    }

    public List<TuitionPaymentResponse> getByClassId(Long classId) {
        return tuitionPaymentRepository.findByStudentClassRoomIdOrderByCreatedAtDesc(classId).stream().map(this::toResponse).collect(Collectors.toList());
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

    private BigDecimal toRemaining(Student s) {
        BigDecimal classFee = s.getClassRoom() != null && s.getClassRoom().getTuitionFee() != null
                ? s.getClassRoom().getTuitionFee()
                : BigDecimal.ZERO;
        BigDecimal paid = s.getTuitionPaidAmount() != null ? s.getTuitionPaidAmount() : BigDecimal.ZERO;
        BigDecimal remaining = classFee.subtract(paid);
        return remaining.compareTo(BigDecimal.ZERO) < 0 ? BigDecimal.ZERO : remaining;
    }
}