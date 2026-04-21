package fit.tedu.HeThong.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UserProfileResponse {
    private Long id;
    private String username;
    private String email;
    private String fullName;
    private String role;
    private boolean enabled;
    
    // For students
    private Long studentId;
    private LocalDate dob;
    private String gender;
    private String address;
    private String phone;
    private String parentName;
    private String parentPhone;
    private String status;
    private Long classId;
    private String className;
    
    // For teachers
    private Long teacherId;
    private String subject;
}
