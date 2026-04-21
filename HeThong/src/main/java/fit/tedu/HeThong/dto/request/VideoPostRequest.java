package fit.tedu.HeThong.dto.request;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class VideoPostRequest {
    @NotBlank
    private String title;

    @NotBlank
    private String videoUrl;

    private String thumbnailUrl;
    private String content;
    private Boolean published = true;
}
