package fit.tedu.HeThong.controller;

import fit.tedu.HeThong.dto.request.VideoPostRequest;
import fit.tedu.HeThong.dto.response.ApiResponse;
import fit.tedu.HeThong.dto.response.VideoPostResponse;
import fit.tedu.HeThong.service.VideoPostService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/posts")
@RequiredArgsConstructor
public class VideoPostController {

    private final VideoPostService videoPostService;

    @GetMapping
    public ResponseEntity<ApiResponse<List<VideoPostResponse>>> getPosts(
            @RequestParam(defaultValue = "false") boolean all,
            @RequestParam(defaultValue = "false") boolean includeDeleted,
            Authentication authentication) {
        List<VideoPostResponse> data;
        if (all) {
            boolean elevated = authentication != null && authentication.getAuthorities().stream()
                    .anyMatch(a -> a.getAuthority().equals("ADMIN") || a.getAuthority().equals("CONTENT_MANAGER"));
            if (!elevated) {
                return ResponseEntity.status(403).body(ApiResponse.error("Bạn không có quyền xem toàn bộ bài đăng"));
            }
            data = videoPostService.getAllPosts(includeDeleted);
        } else {
            data = videoPostService.getPublishedPosts();
        }
        return ResponseEntity.ok(ApiResponse.ok(data));
    }

    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<VideoPostResponse>> getById(@PathVariable Long id) {
        return ResponseEntity.ok(ApiResponse.ok(videoPostService.getById(id)));
    }

    @PostMapping
    @PreAuthorize("hasAnyAuthority('ADMIN','CONTENT_MANAGER')")
    public ResponseEntity<ApiResponse<VideoPostResponse>> create(
            @Valid @RequestBody VideoPostRequest request,
            Authentication authentication) {
        try {
            return ResponseEntity.ok(ApiResponse.ok("Tạo bài đăng thành công",
                    videoPostService.create(request, authentication.getName())));
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(ApiResponse.error(e.getMessage()));
        }
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAnyAuthority('ADMIN','CONTENT_MANAGER')")
    public ResponseEntity<ApiResponse<VideoPostResponse>> update(
            @PathVariable Long id,
            @Valid @RequestBody VideoPostRequest request) {
        try {
            return ResponseEntity.ok(ApiResponse.ok("Cập nhật bài đăng thành công",
                    videoPostService.update(id, request)));
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(ApiResponse.error(e.getMessage()));
        }
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasAnyAuthority('ADMIN','CONTENT_MANAGER')")
    public ResponseEntity<ApiResponse<Void>> delete(@PathVariable Long id) {
        videoPostService.delete(id);
        return ResponseEntity.ok(ApiResponse.ok("Xóa bài đăng thành công", null));
    }

    @PatchMapping("/{id}/visibility")
    @PreAuthorize("hasAnyAuthority('ADMIN','CONTENT_MANAGER')")
    public ResponseEntity<ApiResponse<VideoPostResponse>> toggleVisibility(@PathVariable Long id) {
        try {
            return ResponseEntity.ok(ApiResponse.ok("Cập nhật trạng thái hiển thị thành công",
                    videoPostService.toggleVisibility(id)));
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(ApiResponse.error(e.getMessage()));
        }
    }

    @PatchMapping("/{id}/pinned")
    @PreAuthorize("hasAnyAuthority('ADMIN','CONTENT_MANAGER')")
    public ResponseEntity<ApiResponse<VideoPostResponse>> togglePinned(@PathVariable Long id) {
        try {
            return ResponseEntity.ok(ApiResponse.ok("Cập nhật ghim bài thành công",
                    videoPostService.togglePinned(id)));
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(ApiResponse.error(e.getMessage()));
        }
    }

    @PatchMapping("/{id}/restore")
    @PreAuthorize("hasAnyAuthority('ADMIN','CONTENT_MANAGER')")
    public ResponseEntity<ApiResponse<VideoPostResponse>> restore(@PathVariable Long id) {
        try {
            return ResponseEntity.ok(ApiResponse.ok("Khôi phục bài đăng thành công",
                    videoPostService.restore(id)));
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(ApiResponse.error(e.getMessage()));
        }
    }
}
