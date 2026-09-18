package fit.tedu.HeThong.controller;
import fit.tedu.HeThong.dto.request.TeachingSessionRequest;
import fit.tedu.HeThong.dto.response.*;
import fit.tedu.HeThong.service.TeachingSessionService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;
import java.time.LocalDate;
import java.util.*;

@RestController @RequestMapping("/api/teaching-sessions") @RequiredArgsConstructor
public class TeachingSessionController {
    private final TeachingSessionService service;
    @GetMapping
    public ApiResponse<List<TeachingSessionResponse>> week(@RequestParam LocalDate from, @RequestParam LocalDate to) {
        return ApiResponse.ok(service.week(from, to));
    }
    @GetMapping("/stats")
    public ApiResponse<Map<String,Object>> stats(@RequestParam LocalDate from, @RequestParam LocalDate to) {
        return ApiResponse.ok(service.stats(from, to));
    }
    @PutMapping("/{id}")
    public ApiResponse<TeachingSessionResponse> update(@PathVariable Long id, @RequestBody TeachingSessionRequest request) {
        return ApiResponse.ok("Đã cập nhật buổi dạy", service.update(id, request));
    }
}
