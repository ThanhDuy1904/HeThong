package fit.tedu.HeThong.dto.response;

import lombok.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ClassResponse {
    private Long id;
    private String className;
    private String grade;
    private String schoolYear;
    private BigDecimal tuitionFee;
    private Long teacherId;
    private String teacherName;
    private long studentCount;
    private boolean archived;
    private LocalDateTime createdAt;
}
