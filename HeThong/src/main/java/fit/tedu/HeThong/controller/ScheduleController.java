package fit.tedu.HeThong.controller;

import fit.tedu.HeThong.dto.request.ScheduleRequest;
import fit.tedu.HeThong.dto.response.ApiResponse;
import fit.tedu.HeThong.dto.response.ScheduleResponse;
import fit.tedu.HeThong.service.ScheduleService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/schedules")
@RequiredArgsConstructor
public class ScheduleController {

    private final ScheduleService scheduleService;

    @GetMapping("/my")
    public ResponseEntity<ApiResponse<List<ScheduleResponse>>> getMySchedule() {
        try {
            org.springframework.security.core.Authentication auth = 
                org.springframework.security.core.context.SecurityContextHolder.getContext().getAuthentication();
            
            if (auth == null || !auth.isAuthenticated() || "anonymousUser".equals(auth.getPrincipal())) {
                return ResponseEntity.status(401).body(ApiResponse.error("Chưa đăng nhập"));
            }
            
            String username = auth.getName();
            return ResponseEntity.ok(ApiResponse.ok(scheduleService.getMySchedule(username)));
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(ApiResponse.error(e.getMessage()));
        }
    }

    @GetMapping("/class/{classId}")
    public ResponseEntity<ApiResponse<List<ScheduleResponse>>> getByClass(@PathVariable Long classId) {
        return ResponseEntity.ok(ApiResponse.ok(scheduleService.getSchedulesByClass(classId)));
    }

    @GetMapping("/teacher/{teacherId}")
    @PreAuthorize("hasAnyAuthority('ADMIN','TEACHER')")
    public ResponseEntity<ApiResponse<List<ScheduleResponse>>> getByTeacher(@PathVariable Long teacherId) {
        return ResponseEntity.ok(ApiResponse.ok(scheduleService.getSchedulesByTeacher(teacherId)));
    }

    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<ScheduleResponse>> getById(@PathVariable Long id) {
        return ResponseEntity.ok(ApiResponse.ok(scheduleService.getById(id)));
    }

    @PostMapping
    @PreAuthorize("hasAuthority('ADMIN')")
    public ResponseEntity<ApiResponse<ScheduleResponse>> create(@Valid @RequestBody ScheduleRequest request) {
        try {
            return ResponseEntity.ok(ApiResponse.ok("Thêm lịch học thành công", scheduleService.create(request)));
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(ApiResponse.error(e.getMessage()));
        }
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAuthority('ADMIN')")
    public ResponseEntity<ApiResponse<ScheduleResponse>> update(
            @PathVariable Long id, @Valid @RequestBody ScheduleRequest request) {
        try {
            return ResponseEntity.ok(ApiResponse.ok("Cập nhật lịch học thành công", scheduleService.update(id, request)));
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(ApiResponse.error(e.getMessage()));
        }
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasAuthority('ADMIN')")
    public ResponseEntity<ApiResponse<Void>> delete(@PathVariable Long id) {
        scheduleService.delete(id);
        return ResponseEntity.ok(ApiResponse.ok("Xóa lịch học thành công", null));
    }
}
