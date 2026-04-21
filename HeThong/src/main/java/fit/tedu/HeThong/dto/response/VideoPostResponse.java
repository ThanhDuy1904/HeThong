package fit.tedu.HeThong.dto.response;

import lombok.*;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class VideoPostResponse {
    private Long id;
    private String title;
    private String postType;
    private String videoUrl;
    private String thumbnailUrl;
    private String category;
    private String content;
    private boolean published;
    private boolean pinned;
    private boolean visible;
    private boolean deleted;
    private String createdByName;
    private LocalDateTime createdAt;
}
