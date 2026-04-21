package fit.tedu.HeThong.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "video_posts")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class VideoPost {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 200)
    private String title;

    @Column(name = "video_url", nullable = false, length = 500)
    private String videoUrl;

    @Column(name = "thumbnail_url", length = 500)
    private String thumbnailUrl;

    @Column(name = "content", columnDefinition = "TEXT")
    private String content;

    @Column(nullable = false)
    @Builder.Default
    private boolean published = true;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "created_by")
    private User createdBy;

    @Column(name = "created_at")
    @Builder.Default
    private LocalDateTime createdAt = LocalDateTime.now();
}
