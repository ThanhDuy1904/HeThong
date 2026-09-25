package fit.tedu.HeThong.dto.response;

import lombok.*;
import java.math.BigDecimal;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RevenueClassResponse {
    private String className;
    private String schoolYear;
    private BigDecimal totalDue;
    private BigDecimal collected;
    private BigDecimal remaining;
    private BigDecimal uncollected;
}
