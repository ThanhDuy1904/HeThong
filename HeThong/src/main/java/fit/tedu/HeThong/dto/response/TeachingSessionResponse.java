package fit.tedu.HeThong.dto.response;
import lombok.Builder;
import lombok.Data;
import java.time.LocalDate;

@Data
@Builder
public class TeachingSessionResponse {
    private Long id, scheduleId, classId, teacherId, substituteTeacherId;
    private String className, subject, teacherName, substituteTeacherName, status, note, startTime, endTime;
    private LocalDate date;
}
