package fit.tedu.HeThong.dto.request;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;
import java.math.BigDecimal;

@Data
public class ClassRequest {
    @NotBlank
    private String className;
    private String grade;
    private String schoolYear;
    private BigDecimal tuitionFee;
    private Long teacherId;
}
