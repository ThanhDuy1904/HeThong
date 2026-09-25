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
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import java.io.ByteArrayOutputStream;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.PDPageContentStream;
import org.apache.pdfbox.pdmodel.font.PDType0Font;
import java.io.File;

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
    public ArchiveFileResponse closeClass(Long classId, String requestedFileName, String format, String username) {
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
        String fileName = requestedFileName.trim();
        if (fileName.isBlank()) throw new RuntimeException("Tên file không hợp lệ");
        boolean excel = "xlsx".equalsIgnoreCase(format) || "excel".equalsIgnoreCase(format);
        String extension = excel ? ".xlsx" : ".pdf";
        fileName = fileName.replaceAll("[^\\p{L}\\p{N}._-]", "_");
        if (!fileName.toLowerCase(Locale.ROOT).endsWith(extension)) fileName += extension;
        byte[] content = excel ? buildTuitionWorkbook(className, fee, students) : buildReceiptPdf(className, fee, students);
        String contentType = excel
                ? "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"
                : "application/pdf";
        ArchiveFileResponse archived = archiveService.saveGenerated(fileName, content, contentType, username);
        return archived;
    }

    private byte[] buildTuitionWorkbook(String className, BigDecimal fee, List<Student> students) {
        try (Workbook workbook = new XSSFWorkbook(); ByteArrayOutputStream output = new ByteArrayOutputStream()) {
            Sheet sheet = workbook.createSheet("Học phí");
            Row title = sheet.createRow(0);
            title.createCell(0).setCellValue("BẢNG TỔNG HỢP HỌC PHÍ");
            title.createCell(1).setCellValue("Lớp: " + className);
            title.createCell(2).setCellValue("Thời gian: " + LocalDateTime.now().toString());
            Row header = sheet.createRow(2);
            String[] columns = {"STT", "Họ và tên", "Lớp", "Học phí", "Đã đóng", "Còn lại", "Trạng thái"};
            for (int i = 0; i < columns.length; i++) header.createCell(i).setCellValue(columns[i]);
            int rowIndex = 3;
            int number = 1;
            for (Student student : students) {
                Row row = sheet.createRow(rowIndex++);
                row.createCell(0).setCellValue(number++);
                row.createCell(1).setCellValue(student.getFullName());
                row.createCell(2).setCellValue(className);
                row.createCell(3).setCellValue(fee.doubleValue());
                row.createCell(4).setCellValue(fee.doubleValue());
                row.createCell(5).setCellValue(0);
                row.createCell(6).setCellValue("Đã đóng đủ");
            }
            for (int i = 0; i < columns.length; i++) sheet.autoSizeColumn(i);
            workbook.write(output);
            return output.toByteArray();
        } catch (Exception e) {
            throw new RuntimeException("Không thể tạo file Excel học phí", e);
        }
    }

    private byte[] buildReceiptPdf(String className, BigDecimal fee, List<Student> students) {
        File fontFile = new File("C:\\Windows\\Fonts\\arial.ttf");
        if (!fontFile.exists()) fontFile = new File("C:\\Windows\\Fonts\\tahoma.ttf");
        if (!fontFile.exists()) throw new RuntimeException("Không tìm thấy font Unicode để tạo PDF");
        try (PDDocument document = new PDDocument(); ByteArrayOutputStream output = new ByteArrayOutputStream()) {
            PDType0Font font = PDType0Font.load(document, fontFile);
            PDPage page = new PDPage();
            document.addPage(page);
            PDPageContentStream stream = new PDPageContentStream(document, page);
            stream.beginText();
            stream.setFont(font, 16);
            stream.newLineAtOffset(50, 780);
            stream.showText("BẢNG TỔNG HỢP HỌC PHÍ");
            stream.setFont(font, 11);
            stream.newLineAtOffset(0, -24);
            stream.showText("Lớp: " + className);
            stream.newLineAtOffset(0, -16);
            stream.showText("Học phí mỗi học sinh: " + fee.toPlainString() + " VNĐ");
            stream.newLineAtOffset(0, -16);
            stream.showText("Thời gian: " + LocalDateTime.now());
            stream.newLineAtOffset(0, -28);
            for (int i = 0; i < students.size(); i++) {
                if (i > 0 && i % 38 == 0) {
                    stream.endText();
                    stream.close();
                    page = new PDPage();
                    document.addPage(page);
                    stream = new PDPageContentStream(document, page);
                    stream.beginText();
                    stream.setFont(font, 11);
                    stream.newLineAtOffset(50, 780);
                }
                stream.showText((i + 1) + ". " + students.get(i).getFullName() + " - ĐÃ ĐÓNG ĐỦ");
                stream.newLineAtOffset(0, -16);
            }
            stream.endText();
            stream.close();
            document.save(output);
            return output.toByteArray();
        } catch (Exception e) {
            throw new RuntimeException("Không thể tạo file PDF học phí", e);
        }
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