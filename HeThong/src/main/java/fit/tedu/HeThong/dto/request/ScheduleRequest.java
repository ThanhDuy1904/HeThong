package fit.tedu.HeThong.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class ScheduleRequest {

    @NotNull(message = "Class ID không được để trống")
    private Long classId;

    @NotBlank(message = "Môn học không được để trống")
    private String subject;

    @NotNull(message = "Thứ không được để trống")
    private Integer dayOfWeek; // 2-7, 1

    @NotBlank(message = "Giờ bắt đầu không được để trống")
    private String startTime; // Format: "HH:mm"

    @NotBlank(message = "Giờ kết thúc không được để trống")
    private String endTime; // Format: "HH:mm"

    private String room;

    private Long teacherId;
}
