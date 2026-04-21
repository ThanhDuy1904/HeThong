package fit.tedu.HeThong.controller;

import fit.tedu.HeThong.dto.request.AttendanceRequest;
import fit.tedu.HeThong.dto.response.ApiResponse;
import fit.tedu.HeThong.dto.response.AttendanceResponse;
import fit.tedu.HeThong.entity.Student;
import fit.tedu.HeThong.service.AttendanceService;
import fit.tedu.HeThong.repository.StudentRepository;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/attendances")
@RequiredArgsConstructor
public class AttendanceController {

    private final AttendanceService attendanceService;
    private final fit.tedu.HeThong.repository.TeacherRepository teacherRepository;
    private final StudentRepository studentRepository;

    @GetMapping("/schedule/{scheduleId}")
    @PreAuthorize("hasAnyAuthority('ADMIN','TEACHER')")
    public ResponseEntity<ApiResponse<List<AttendanceResponse>>> getByScheduleAndDate(
            @PathVariable Long scheduleId,
            @RequestParam String date) {
        return ResponseEntity.ok(ApiResponse.ok(
                attendanceService.getAttendancesByScheduleAndDate(scheduleId, date)));
    }

    @GetMapping("/student/{studentId}")
    @PreAuthorize("hasAnyAuthority('ADMIN','TEACHER','STUDENT')")
    public ResponseEntity<ApiResponse<List<AttendanceResponse>>> getByStudent(@PathVariable Long studentId) {
        org.springframework.security.core.Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        boolean isElevated = auth != null && auth.getAuthorities().stream()
                .anyMatch(a -> a.getAuthority().equals("ADMIN") || a.getAuthority().equals("TEACHER"));
        if (!isElevated) {
            Student currentStudent = studentRepository.findByUsername(auth.getName())
                    .orElseThrow(() -> new RuntimeException("Không tìm thấy thông tin học sinh"));
            if (!currentStudent.getId().equals(studentId)) {
                return ResponseEntity.status(403).body(ApiResponse.error("Bạn chỉ được xem lịch sử điểm danh của chính mình"));
            }
        }
        return ResponseEntity.ok(ApiResponse.ok(attendanceService.getAttendancesByStudent(studentId)));
    }

    @PostMapping("/mark")
    @PreAuthorize("hasAnyAuthority('ADMIN','TEACHER')")
    public ResponseEntity<ApiResponse<List<AttendanceResponse>>> markAttendance(
            @Valid @RequestBody AttendanceRequest request,
            @AuthenticationPrincipal UserDetails userDetails) {
        try {
            // Lấy teacherId từ user đang đăng nhập
            String username = userDetails.getUsername();
            Long teacherId = teacherRepository.findByUsername(username)
                    .map(fit.tedu.HeThong.entity.Teacher::getId)
                    .orElse(null); // Nếu không phải teacher thì null (có thể là admin)
            
            return ResponseEntity.ok(ApiResponse.ok("Điểm danh thành công", 
                    attendanceService.markAttendance(request, teacherId)));
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(ApiResponse.error(e.getMessage()));
        }
    }
}
