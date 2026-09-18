package fit.tedu.HeThong.dto.response;

import lombok.Builder;
import lombok.Data;
import java.time.LocalDateTime;

@Data @Builder
public class ArchiveFileResponse {
    private Long id;
    private String originalName;
    private String contentType;
    private long size;
    private String uploadedBy;
    private LocalDateTime createdAt;
}
