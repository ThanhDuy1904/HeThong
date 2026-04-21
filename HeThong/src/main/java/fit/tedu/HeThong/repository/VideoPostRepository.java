package fit.tedu.HeThong.repository;

import fit.tedu.HeThong.entity.VideoPost;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface VideoPostRepository extends JpaRepository<VideoPost, Long> {
    List<VideoPost> findByPublishedTrueOrderByCreatedAtDesc();
}
