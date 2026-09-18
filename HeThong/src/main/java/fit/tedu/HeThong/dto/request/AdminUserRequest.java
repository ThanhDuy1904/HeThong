package fit.tedu.HeThong.dto.request;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class AdminUserRequest {
    @NotBlank
    @Size(min = 3, max = 50)
    private String username;

    @Size(min = 6, max = 100)
    private String password;

    @Email
    private String email;

    @NotBlank
    private String fullName;

    @NotBlank
    private String role;

    private boolean enabled = true;
}
