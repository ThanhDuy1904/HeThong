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
            @RequestParam(defaultValue = "false") boolean all) {
        List<VideoPostResponse> data = all ? videoPostService.getAllPosts() : videoPostService.getPublishedPosts();
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
}
