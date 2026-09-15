package fit.tedu.HeThong.dto.request;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

@Data
public class StudentRequest {
    @NotBlank
    private String fullName;
    private LocalDate dob;
    private String gender;
    private String address;
    private String phone;
    private String parentName;
    private String parentPhone;
    private Long classId;
    private List<Long> classIds;
    private String status = "ACTIVE";
    private BigDecimal tuitionPaidAmount;
    private Boolean tuitionPaidFull;
    // Optional: create a linked user account
    private String username;
    private String password;
    private String email;
}
