package fit.tedu.HeThong.controller;

import fit.tedu.HeThong.dto.response.*;
import fit.tedu.HeThong.service.ExpenseService;
import lombok.RequiredArgsConstructor;
import org.springframework.core.io.*;
import org.springframework.http.*;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import java.math.BigDecimal;
import java.nio.file.Path;
import java.util.List;

@RestController
@RequestMapping("/api/expenses")
@RequiredArgsConstructor
public class ExpenseController {
    private final ExpenseService service;
    @GetMapping public ApiResponse<List<ExpenseResponse>> list() { return ApiResponse.ok(service.list()); }
    @GetMapping("/available") public ApiResponse<BigDecimal> available() { return ApiResponse.ok(service.availableAmount()); }
    @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ApiResponse<ExpenseResponse> create(@RequestParam String category, @RequestParam(required=false) String description,
            @RequestParam int year, @RequestParam int month, @RequestParam BigDecimal amount,
            @RequestPart(required=false) MultipartFile image, java.security.Principal principal) {
        return ApiResponse.ok("Đã lưu phiếu chi", service.create(category, description, year, month, amount, image, principal.getName()));
    }
    @DeleteMapping("/{id}") public ApiResponse<Void> delete(@PathVariable Long id) { service.delete(id); return ApiResponse.ok("Đã xóa phiếu chi", null); }
    @GetMapping("/{id}/image") public ResponseEntity<Resource> image(@PathVariable Long id) throws Exception {
        PathResource resource = new PathResource(service.imagePath(id));
        if (!resource.exists()) throw new RuntimeException("Hình ảnh không còn tồn tại");
        return ResponseEntity.ok().contentType(MediaType.parseMediaType(service.imageType(id) == null ? "application/octet-stream" : service.imageType(id))).body(resource);
    }
}
