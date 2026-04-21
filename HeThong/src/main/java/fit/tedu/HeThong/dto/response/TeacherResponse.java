package fit.tedu.HeThong.dto.response;

import lombok.*;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TeacherResponse {
    private Long id;
    private String fullName;
    private LocalDate dob;
    private String gender;
    private String address;
    private String phone;
    private String subject;
    private Long userId;
    private String username;
    private LocalDateTime createdAt;
}
