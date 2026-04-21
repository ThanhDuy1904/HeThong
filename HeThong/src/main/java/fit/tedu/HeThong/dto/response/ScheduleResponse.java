package fit.tedu.HeThong.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ScheduleResponse {

    private Long id;
    private Long classId;
    private String className;
    private String subject;
    private Integer dayOfWeek;
    private String dayOfWeekText; // "Thứ 2", "Thứ 3", ...
    private String startTime;
    private String endTime;
    private String room;
    private Long teacherId;
    private String teacherName;
}
