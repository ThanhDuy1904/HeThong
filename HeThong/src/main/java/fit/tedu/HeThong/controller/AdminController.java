package fit.tedu.HeThong.controller;

import fit.tedu.HeThong.dto.response.ApiResponse;
import fit.tedu.HeThong.repository.UserRepository;
import fit.tedu.HeThong.repository.StudentRepository;
import fit.tedu.HeThong.service.*;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.util.Map;

@RestController
@RequestMapping("/api/admin")
@RequiredArgsConstructor
@PreAuthorize("hasAnyAuthority('ADMIN','ACADEMIC_AFFAIRS','MANAGER')")
public class AdminController {

    private final StudentService studentService;
    private final TeacherService teacherService;
    private final ClassService classService;
    private final UserRepository userRepository;
    private final StudentRepository studentRepository;

    @GetMapping("/dashboard")
    public ResponseEntity<ApiResponse<Map<String, Object>>> getDashboard() {
        Map<String, Object> stats = Map.of(
            "totalStudents",  studentService.countAll(),
            "activeStudents", studentService.countByStatus("ACTIVE"),
            "totalTeachers",  teacherService.countAll(),
            "totalClasses",   classService.countAll(),
            "totalUsers",     userRepository.count(),
            "tuitionPaidStudents", studentRepository.countByTuitionPaidFullTrue(),
            "tuitionPendingStudents", studentRepository.countByTuitionPaidFullFalse(),
            "tuitionRemaining", studentRepository.sumTuitionRemaining(),
            "tuitionCollected", studentRepository.sumTuitionPaid()
        );
        return ResponseEntity.ok(ApiResponse.ok(stats));
    }
}
