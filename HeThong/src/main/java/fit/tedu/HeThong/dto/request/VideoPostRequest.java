package fit.tedu.HeThong.dto.request;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class VideoPostRequest {
    @NotBlank
    private String title;

    private String videoUrl;

    private String postType = "ANNOUNCEMENT";
    private String thumbnailUrl;
    private String category;
    private String content;
    private Boolean published = true;
    private Boolean pinned = false;
    private Boolean visible = true;
}
