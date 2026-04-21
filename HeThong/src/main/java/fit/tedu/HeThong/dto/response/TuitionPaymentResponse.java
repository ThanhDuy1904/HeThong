package fit.tedu.HeThong.dto.response;

import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TuitionPaymentResponse {
    private Long id;
    private Long studentId;
    private String studentName;
    private String className;
    private BigDecimal amount;
    private BigDecimal balanceAfter;
    private String note;
    private Long createdById;
    private String createdByName;
    private LocalDateTime createdAt;
}