package fit.tedu.HeThong.controller;

import fit.tedu.HeThong.dto.request.StudentRequest;
import fit.tedu.HeThong.dto.response.ApiResponse;
import fit.tedu.HeThong.dto.response.StudentResponse;
import fit.tedu.HeThong.service.StudentService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/students")
@RequiredArgsConstructor
public class StudentController {

    private final StudentService studentService;

    @GetMapping
    public ResponseEntity<ApiResponse<List<StudentResponse>>> getAll(
            @RequestParam(required = false) String keyword) {
        List<StudentResponse> data = (keyword != null && !keyword.isBlank())
                ? studentService.search(keyword)
                : studentService.getAll();
        return ResponseEntity.ok(ApiResponse.ok(data));
    }

    @GetMapping("/me")
    public ResponseEntity<ApiResponse<StudentResponse>> getMyInfo() {
        try {
            org.springframework.security.core.Authentication auth = 
                org.springframework.security.core.context.SecurityContextHolder.getContext().getAuthentication();
            
            if (auth == null || !auth.isAuthenticated() || "anonymousUser".equals(auth.getPrincipal())) {
                return ResponseEntity.status(401).body(ApiResponse.error("Chưa đăng nhập"));
            }
            
            String username = auth.getName();
            return ResponseEntity.ok(ApiResponse.ok(studentService.getByUsername(username)));
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(ApiResponse.error(e.getMessage()));
        }
    }

    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<StudentResponse>> getById(@PathVariable Long id) {
        return ResponseEntity.ok(ApiResponse.ok(studentService.getById(id)));
    }

    @GetMapping("/class/{classId}")
    public ResponseEntity<ApiResponse<List<StudentResponse>>> getByClass(@PathVariable Long classId) {
        return ResponseEntity.ok(ApiResponse.ok(studentService.getByClass(classId)));
    }

    @PostMapping
    @PreAuthorize("hasAnyAuthority('ADMIN','TEACHER','ACCOUNTANT')")
    public ResponseEntity<ApiResponse<StudentResponse>> create(@Valid @RequestBody StudentRequest request) {
        try {
            return ResponseEntity.ok(ApiResponse.ok("Thêm học sinh thành công", studentService.create(request)));
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(ApiResponse.error(e.getMessage()));
        }
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAnyAuthority('ADMIN','TEACHER','ACCOUNTANT')")
    public ResponseEntity<ApiResponse<StudentResponse>> update(
            @PathVariable Long id, @Valid @RequestBody StudentRequest request) {
        try {
            return ResponseEntity.ok(ApiResponse.ok("Cập nhật thành công", studentService.update(id, request)));
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(ApiResponse.error(e.getMessage()));
        }
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasAuthority('ADMIN')")
    public ResponseEntity<ApiResponse<Void>> delete(@PathVariable Long id) {
        studentService.delete(id);
        return ResponseEntity.ok(ApiResponse.ok("Xóa học sinh thành công", null));
    }

    @PatchMapping("/{id}/remove-from-class")
    @PreAuthorize("hasAnyAuthority('ADMIN','TEACHER','ACCOUNTANT')")
    public ResponseEntity<ApiResponse<StudentResponse>> removeFromClass(@PathVariable Long id) {
        try {
            return ResponseEntity.ok(ApiResponse.ok("Loại bỏ học sinh khỏi lớp thành công", 
                    studentService.removeFromClass(id)));
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(ApiResponse.error(e.getMessage()));
        }
    }
}
