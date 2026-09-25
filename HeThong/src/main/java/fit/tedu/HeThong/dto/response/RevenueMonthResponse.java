package fit.tedu.HeThong.dto.response;

import lombok.*;
import java.math.BigDecimal;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RevenueMonthResponse {
    private int month;
    private BigDecimal collected;
}
