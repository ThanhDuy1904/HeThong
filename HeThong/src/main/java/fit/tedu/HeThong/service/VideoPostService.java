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
        return videoPostRepository.findByPublishedTrueOrderByCreatedAtDesc().stream().map(this::toResponse).toList();
    }

    public List<VideoPostResponse> getAllPosts() {
        return videoPostRepository.findAll().stream().map(this::toResponse).toList();
    }

    public VideoPostResponse getById(Long id) {
        return toResponse(videoPostRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy bài đăng video")));
    }

    @Transactional
    public VideoPostResponse create(VideoPostRequest request, String username) {
        User creator = userRepository.findByUsername(username)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy người tạo bài"));

        VideoPost post = VideoPost.builder()
                .title(request.getTitle())
                .videoUrl(request.getVideoUrl())
                .thumbnailUrl(request.getThumbnailUrl())
                .content(request.getContent())
                .published(Boolean.TRUE.equals(request.getPublished()))
                .createdBy(creator)
                .build();

        return toResponse(videoPostRepository.save(post));
    }

    @Transactional
    public VideoPostResponse update(Long id, VideoPostRequest request) {
        VideoPost post = videoPostRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy bài đăng video"));

        post.setTitle(request.getTitle());
        post.setVideoUrl(request.getVideoUrl());
        post.setThumbnailUrl(request.getThumbnailUrl());
        post.setContent(request.getContent());
        post.setPublished(Boolean.TRUE.equals(request.getPublished()));

        return toResponse(videoPostRepository.save(post));
    }

    @Transactional
    public void delete(Long id) {
        videoPostRepository.deleteById(id);
    }

    private VideoPostResponse toResponse(VideoPost post) {
        return VideoPostResponse.builder()
                .id(post.getId())
                .title(post.getTitle())
                .videoUrl(post.getVideoUrl())
                .thumbnailUrl(post.getThumbnailUrl())
                .content(post.getContent())
                .published(post.isPublished())
                .createdByName(post.getCreatedBy() != null ? post.getCreatedBy().getFullName() : null)
                .createdAt(post.getCreatedAt())
                .build();
    }
}
