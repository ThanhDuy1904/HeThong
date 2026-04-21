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
    private String videoUrl;
    private String thumbnailUrl;
    private String content;
    private boolean published;
    private String createdByName;
    private LocalDateTime createdAt;
}
