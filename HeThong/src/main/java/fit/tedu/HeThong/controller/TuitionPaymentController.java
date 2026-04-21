package fit.tedu.HeThong.controller;

import fit.tedu.HeThong.dto.request.TuitionPaymentCollectRequest;
import fit.tedu.HeThong.dto.response.ApiResponse;
import fit.tedu.HeThong.dto.response.StudentResponse;
import fit.tedu.HeThong.dto.response.TuitionPaymentResponse;
import fit.tedu.HeThong.entity.Student;
import fit.tedu.HeThong.repository.StudentRepository;
import fit.tedu.HeThong.service.TuitionPaymentService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;
import jakarta.validation.Valid;

import java.util.List;

@RestController
@RequestMapping("/api/tuition-payments")
@RequiredArgsConstructor
public class TuitionPaymentController {

    private final TuitionPaymentService tuitionPaymentService;
    private final StudentRepository studentRepository;

    @GetMapping("/me")
    public ResponseEntity<ApiResponse<List<TuitionPaymentResponse>>> getMyHistory() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || !auth.isAuthenticated() || "anonymousUser".equals(auth.getPrincipal())) {
            return ResponseEntity.status(401).body(ApiResponse.error("Chưa đăng nhập"));
        }
        Student student = studentRepository.findByUsername(auth.getName())
                .orElseThrow(() -> new RuntimeException("Không tìm thấy thông tin học sinh"));
        return ResponseEntity.ok(ApiResponse.ok(tuitionPaymentService.getByStudentId(student.getId())));
    }

    @GetMapping("/student/{studentId}")
    public ResponseEntity<ApiResponse<List<TuitionPaymentResponse>>> getByStudent(@PathVariable Long studentId) {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        boolean elevated = auth != null && auth.getAuthorities().stream().anyMatch(a -> a.getAuthority().equals("ADMIN") || a.getAuthority().equals("ACCOUNTANT"));
        if (!elevated) {
            Student student = studentRepository.findByUsername(auth.getName())
                    .orElseThrow(() -> new RuntimeException("Không tìm thấy thông tin học sinh"));
            if (!student.getId().equals(studentId)) {
                return ResponseEntity.status(403).body(ApiResponse.error("Bạn chỉ được xem lịch sử học phí của chính mình"));
            }
        }
        return ResponseEntity.ok(ApiResponse.ok(tuitionPaymentService.getByStudentId(studentId)));
    }

    @GetMapping("/class/{classId}")
    public ResponseEntity<ApiResponse<List<TuitionPaymentResponse>>> getByClass(@PathVariable Long classId) {
        return ResponseEntity.ok(ApiResponse.ok(tuitionPaymentService.getByClassId(classId)));
    }

    @PostMapping("/student/{studentId}/collect")
    public ResponseEntity<ApiResponse<StudentResponse>> collectPayment(
            @PathVariable Long studentId,
            @Valid @RequestBody TuitionPaymentCollectRequest request,
            Authentication authentication) {
        try {
            boolean elevated = authentication != null && authentication.getAuthorities().stream()
                    .anyMatch(a -> a.getAuthority().equals("ADMIN") || a.getAuthority().equals("ACCOUNTANT"));
            if (!elevated) {
                return ResponseEntity.status(403).body(ApiResponse.error("Bạn không có quyền thu học phí"));
            }
            return ResponseEntity.ok(ApiResponse.ok("Thu học phí thành công",
                    tuitionPaymentService.collectPayment(studentId, request.getAmount(), request.getNote(), authentication.getName())));
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(ApiResponse.error(e.getMessage()));
        }
    }
}