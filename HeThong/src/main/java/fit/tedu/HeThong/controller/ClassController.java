package fit.tedu.HeThong.controller;

import fit.tedu.HeThong.dto.request.ClassRequest;
import fit.tedu.HeThong.dto.response.ApiResponse;
import fit.tedu.HeThong.dto.response.ClassResponse;
import fit.tedu.HeThong.service.ClassService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.security.core.Authentication;

import java.math.BigDecimal;
import java.util.List;

@RestController
@RequestMapping("/api/classes")
@RequiredArgsConstructor
public class ClassController {

    private final ClassService classService;

    @GetMapping
    public ResponseEntity<ApiResponse<List<ClassResponse>>> getAll(Authentication authentication) {
        if (authentication != null && authentication.getAuthorities().stream()
                .anyMatch(a -> a.getAuthority().equals("TEACHER"))) {
            return ResponseEntity.ok(ApiResponse.ok(classService.getByTeacherUsername(authentication.getName())));
        }
        return ResponseEntity.ok(ApiResponse.ok(classService.getAll()));
    }

    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<ClassResponse>> getById(@PathVariable Long id, Authentication authentication) {
        if (authentication != null && authentication.getAuthorities().stream()
                .anyMatch(a -> a.getAuthority().equals("TEACHER"))) {
            return ResponseEntity.ok(ApiResponse.ok(
                    classService.getByIdForTeacher(id, authentication.getName())));
        }
        return ResponseEntity.ok(ApiResponse.ok(classService.getById(id)));
    }

    @PostMapping
    @PreAuthorize("hasAnyAuthority('ADMIN','ACADEMIC_AFFAIRS')")
    public ResponseEntity<ApiResponse<ClassResponse>> create(@Valid @RequestBody ClassRequest request) {
        try {
            return ResponseEntity.ok(ApiResponse.ok("Thêm lớp thành công", classService.create(request)));
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(ApiResponse.error(e.getMessage()));
        }
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAnyAuthority('ADMIN','ACADEMIC_AFFAIRS')")
    public ResponseEntity<ApiResponse<ClassResponse>> update(
            @PathVariable Long id, @Valid @RequestBody ClassRequest request) {
        try {
            return ResponseEntity.ok(ApiResponse.ok("Cập nhật thành công", classService.update(id, request)));
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(ApiResponse.error(e.getMessage()));
        }
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasAnyAuthority('ADMIN','ACADEMIC_AFFAIRS')")
    public ResponseEntity<ApiResponse<Void>> delete(@PathVariable Long id) {
        classService.delete(id);
        return ResponseEntity.ok(ApiResponse.ok("Xóa lớp thành công", null));
    }

    @PutMapping("/{id}/tuition-fee")
    @PreAuthorize("hasAnyAuthority('ADMIN','ACCOUNTANT')")
    public ResponseEntity<ApiResponse<ClassResponse>> updateTuitionFee(
            @PathVariable Long id, @RequestParam BigDecimal tuitionFee) {
        try {
            return ResponseEntity.ok(ApiResponse.ok("Cập nhật học phí thành công",
                    classService.updateTuitionFee(id, tuitionFee)));
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(ApiResponse.error(e.getMessage()));
        }
    }
}
