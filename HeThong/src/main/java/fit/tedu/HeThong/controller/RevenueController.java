package fit.tedu.HeThong.controller;

import fit.tedu.HeThong.dto.response.*;
import fit.tedu.HeThong.service.RevenueService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/revenue")
@RequiredArgsConstructor
public class RevenueController {
    private final RevenueService revenueService;

    @GetMapping
    public ApiResponse<RevenueReportResponse> report(
            @RequestParam(required = false) String schoolYear,
            @RequestParam(required = false) Integer year,
            @RequestParam(required = false) Integer month) {
        return ApiResponse.ok(revenueService.report(schoolYear, year, month));
    }
}
