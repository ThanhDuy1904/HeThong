package fit.tedu.HeThong.service;

import fit.tedu.HeThong.dto.request.VideoPostRequest;
import fit.tedu.HeThong.dto.response.VideoPostResponse;
import fit.tedu.HeThong.entity.User;
import fit.tedu.HeThong.entity.VideoPost;
import fit.tedu.HeThong.repository.UserRepository;
import fit.tedu.HeThong.repository.VideoPostRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class VideoPostService {

    private final VideoPostRepository videoPostRepository;
    private final UserRepository userRepository;

    public List<VideoPostResponse> getPublishedPosts() {
        return videoPostRepository.findByDeletedFalseAndVisibleTrueOrderByPinnedDescCreatedAtDesc().stream().map(this::toResponse).toList();
    }

    public List<VideoPostResponse> getAllPosts(boolean includeDeleted) {
        return (includeDeleted
                ? videoPostRepository.findAllByOrderByDeletedAscPinnedDescVisibleDescCreatedAtDesc()
                : videoPostRepository.findByDeletedFalseOrderByPinnedDescVisibleDescCreatedAtDesc())
                .stream().map(this::toResponse).toList();
    }

    public VideoPostResponse getById(Long id) {
        return toResponse(videoPostRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy bài đăng video")));
    }

    @Transactional
    public VideoPostResponse create(VideoPostRequest request, String username) {
        User creator = userRepository.findByUsername(username)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy người tạo bài"));

        String postType = normalizePostType(request.getPostType());
        String mediaUrl = normalizeMediaUrl(postType, request.getVideoUrl());
        validateMedia(postType, mediaUrl);

        VideoPost post = VideoPost.builder()
                .title(request.getTitle())
                .postType(postType)
                .videoUrl(mediaUrl)
                .thumbnailUrl(request.getThumbnailUrl())
                .category(request.getCategory())
                .content(request.getContent())
                .published(Boolean.TRUE.equals(request.getPublished()))
                .pinned(Boolean.TRUE.equals(request.getPinned()))
                .visible(request.getVisible() == null || request.getVisible())
                .deleted(false)
                .createdBy(creator)
                .build();

        return toResponse(videoPostRepository.save(post));
    }

    @Transactional
    public VideoPostResponse update(Long id, VideoPostRequest request) {
        VideoPost post = videoPostRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy bài đăng video"));

        String postType = normalizePostType(request.getPostType());
        String mediaUrl = normalizeMediaUrl(postType, request.getVideoUrl());
        validateMedia(postType, mediaUrl);

        post.setTitle(request.getTitle());
        post.setPostType(postType);
        post.setVideoUrl(mediaUrl);
        post.setThumbnailUrl(request.getThumbnailUrl());
        post.setCategory(request.getCategory());
        post.setContent(request.getContent());
        post.setPublished(Boolean.TRUE.equals(request.getPublished()));
        post.setPinned(Boolean.TRUE.equals(request.getPinned()));
        post.setVisible(request.getVisible() == null || request.getVisible());

        return toResponse(videoPostRepository.save(post));
    }

    @Transactional
    public void delete(Long id) {
        VideoPost post = videoPostRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy bài đăng video"));
        post.setDeleted(true);
        post.setVisible(false);
        videoPostRepository.save(post);
    }

    @Transactional
    public VideoPostResponse restore(Long id) {
        VideoPost post = videoPostRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy bài đăng video"));
        post.setDeleted(false);
        return toResponse(videoPostRepository.save(post));
    }

    @Transactional
    public VideoPostResponse toggleVisibility(Long id) {
        VideoPost post = videoPostRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy bài đăng video"));
        post.setVisible(!post.isVisible());
        if (post.isDeleted()) {
            post.setDeleted(false);
        }
        return toResponse(videoPostRepository.save(post));
    }

    @Transactional
    public VideoPostResponse togglePinned(Long id) {
        VideoPost post = videoPostRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy bài đăng video"));
        post.setPinned(!post.isPinned());
        return toResponse(videoPostRepository.save(post));
    }

    private VideoPostResponse toResponse(VideoPost post) {
        return VideoPostResponse.builder()
                .id(post.getId())
                .title(post.getTitle())
                .postType(post.getPostType())
                .videoUrl(post.getVideoUrl())
                .thumbnailUrl(post.getThumbnailUrl())
                .category(post.getCategory())
                .content(post.getContent())
                .published(post.isPublished())
                .pinned(post.isPinned())
                .visible(post.isVisible())
                .deleted(post.isDeleted())
                .createdByName(post.getCreatedBy() != null ? post.getCreatedBy().getFullName() : null)
                .createdAt(post.getCreatedAt())
                .build();
    }

    private String normalizePostType(String postType) {
        if (postType == null || postType.isBlank()) {
            return "ANNOUNCEMENT";
        }
        String normalized = postType.trim().toUpperCase();
        return switch (normalized) {
            case "ANNOUNCEMENT", "VIDEO", "IMAGE" -> normalized;
            default -> throw new RuntimeException("Loại bài đăng không hợp lệ");
        };
    }

    private String normalizeMediaUrl(String postType, String mediaUrl) {
        if (mediaUrl == null || mediaUrl.isBlank()) {
            return "ANNOUNCEMENT".equals(postType) ? "" : null;
        }
        return mediaUrl.trim();
    }

    private void validateMedia(String postType, String mediaUrl) {
        if (!"ANNOUNCEMENT".equals(postType) && mediaUrl == null) {
            throw new RuntimeException("Bài video hoặc hình ảnh cần có đường dẫn hoặc file đính kèm");
        }
    }
}
