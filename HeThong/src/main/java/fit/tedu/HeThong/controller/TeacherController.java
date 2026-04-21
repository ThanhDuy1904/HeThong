package fit.tedu.HeThong.controller;

import fit.tedu.HeThong.dto.request.TeacherRequest;
import fit.tedu.HeThong.dto.response.ApiResponse;
import fit.tedu.HeThong.dto.response.TeacherResponse;
import fit.tedu.HeThong.service.TeacherService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/teachers")
@RequiredArgsConstructor
public class TeacherController {

    private final TeacherService teacherService;

    @GetMapping
    public ResponseEntity<ApiResponse<List<TeacherResponse>>> getAll(
            @RequestParam(required = false) String keyword) {
        List<TeacherResponse> data = (keyword != null && !keyword.isBlank())
                ? teacherService.search(keyword)
                : teacherService.getAll();
        return ResponseEntity.ok(ApiResponse.ok(data));
    }

    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<TeacherResponse>> getById(@PathVariable Long id) {
        return ResponseEntity.ok(ApiResponse.ok(teacherService.getById(id)));
    }

    @PostMapping
    @PreAuthorize("hasAuthority('ADMIN')")
    public ResponseEntity<ApiResponse<TeacherResponse>> create(@Valid @RequestBody TeacherRequest request) {
        try {
            return ResponseEntity.ok(ApiResponse.ok("Thêm giáo viên thành công", teacherService.create(request)));
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(ApiResponse.error(e.getMessage()));
        }
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAnyAuthority('ADMIN','TEACHER')")
    public ResponseEntity<ApiResponse<TeacherResponse>> update(
            @PathVariable Long id, @Valid @RequestBody TeacherRequest request) {
        try {
            return ResponseEntity.ok(ApiResponse.ok("Cập nhật thành công", teacherService.update(id, request)));
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(ApiResponse.error(e.getMessage()));
        }
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasAuthority('ADMIN')")
    public ResponseEntity<ApiResponse<Void>> delete(@PathVariable Long id) {
        teacherService.delete(id);
        return ResponseEntity.ok(ApiResponse.ok("Xóa giáo viên thành công", null));
    }
}
