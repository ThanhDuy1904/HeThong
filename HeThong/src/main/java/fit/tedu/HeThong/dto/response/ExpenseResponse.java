package fit.tedu.HeThong.dto.response;

import lombok.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data @Builder @NoArgsConstructor @AllArgsConstructor
public class ExpenseResponse {
    private Long id;
    private String category;
    private String description;
    private int year;
    private int month;
    private BigDecimal amount;
    private String imageName;
    private String createdBy;
    private LocalDateTime createdAt;
}
