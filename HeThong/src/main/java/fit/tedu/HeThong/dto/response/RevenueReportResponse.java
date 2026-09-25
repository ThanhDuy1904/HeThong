package fit.tedu.HeThong.dto.response;

import lombok.*;
import java.math.BigDecimal;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RevenueReportResponse {
    private BigDecimal totalCollected;
    private BigDecimal totalDue;
    private BigDecimal totalRemaining;
    private BigDecimal totalUncollected;
    private BigDecimal totalExpense;
    private List<RevenueClassResponse> byClass;
    private List<RevenueMonthResponse> monthly;
}
