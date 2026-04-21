package fit.tedu.HeThong.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AttendanceResponse {

    private Long id;
    private Long studentId;
    private String studentName;
    private Long scheduleId;
    private String subject;
    private String attendanceDate;
    private String status;
    private String note;
    private String markedByName;
    private String createdAt;
}
