package fit.tedu.HeThong.dto.response;

import lombok.*;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class StudentResponse {
    private Long id;
    private String fullName;
    private LocalDate dob;
    private String gender;
    private String address;
    private String phone;
    private String parentName;
    private String parentPhone;
    private String status;
    private Long classId;
    private String className;
    private BigDecimal classTuitionFee;
    private BigDecimal tuitionPaidAmount;
    private boolean tuitionPaidFull;
    private BigDecimal tuitionRemaining;
    private Long userId;
    private String username;
    private LocalDateTime createdAt;
}
