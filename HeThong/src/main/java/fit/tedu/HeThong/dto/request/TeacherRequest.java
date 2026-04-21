package fit.tedu.HeThong.dto.request;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;
import java.time.LocalDate;

@Data
public class TeacherRequest {
    @NotBlank
    private String fullName;
    private LocalDate dob;
    private String gender;
    private String address;
    private String phone;
    private String subject;
    // Optional: create linked user
    private String username;
    private String password;
    private String email;
}
