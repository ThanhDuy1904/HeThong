package fit.tedu.HeThong.controller;

import fit.tedu.HeThong.dto.response.*;
import fit.tedu.HeThong.entity.ArchiveFile;
import fit.tedu.HeThong.service.ArchiveService;
import lombok.RequiredArgsConstructor;
import org.springframework.core.io.Resource;
import org.springframework.http.*;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import java.util.List;

@RestController @RequestMapping("/api/archive") @RequiredArgsConstructor
@PreAuthorize("hasAnyAuthority('ADMIN','ACADEMIC_AFFAIRS','MANAGER')")
public class ArchiveController {
    private final ArchiveService service;
    @GetMapping public ApiResponse<List<ArchiveFileResponse>> list(java.security.Principal principal) {
        return ApiResponse.ok(service.list(principal.getName()));
    }
    @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ApiResponse<ArchiveFileResponse> upload(@RequestPart("file") MultipartFile file, java.security.Principal principal) {
        return ApiResponse.ok("Tải file lên thành công", service.upload(file, principal.getName()));
    }
    @GetMapping("/{id}/download")
    public ResponseEntity<Resource> download(@PathVariable Long id) {
        ArchiveFile f = service.get(id);
        if (!f.getUploadedBy().equals(currentUsername())) {
            throw new org.springframework.security.access.AccessDeniedException("Bạn không có quyền tải tài liệu này");
        }
        return ResponseEntity.ok().contentType(MediaType.parseMediaType(f.getContentType()))
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + f.getOriginalName().replace("\"", "") + "\"")
                .body(service.download(id));
    }
    private String currentUsername() {
        return org.springframework.security.core.context.SecurityContextHolder.getContext()
                .getAuthentication().getName();
    }
}
