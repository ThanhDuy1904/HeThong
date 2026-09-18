package fit.tedu.HeThong.dto.request;
import lombok.Data;
import java.time.LocalDate;

@Data
public class TeachingSessionRequest {
    private LocalDate date;
    private String status;
    private Long substituteTeacherId;
    private String note;
}
