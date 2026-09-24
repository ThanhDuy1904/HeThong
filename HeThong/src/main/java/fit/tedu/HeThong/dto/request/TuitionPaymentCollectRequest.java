package fit.tedu.HeThong.dto.request;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.math.BigDecimal;

@Data
public class TuitionPaymentCollectRequest {

    @NotNull
    @DecimalMin(value = "0.01")
    private BigDecimal amount;

    private Long classId;

    private String note;
}