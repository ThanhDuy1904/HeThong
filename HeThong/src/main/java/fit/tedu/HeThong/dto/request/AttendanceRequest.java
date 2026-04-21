package fit.tedu.HeThong.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class AttendanceRequest {

    @NotNull(message = "Schedule ID không được để trống")
    private Long scheduleId;

    @NotBlank(message = "Ngày điểm danh không được để trống")
    private String attendanceDate; // Format: "yyyy-MM-dd"

    @NotNull(message = "Danh sách điểm danh không được để trống")
    private List<StudentAttendance> attendances;

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class StudentAttendance {
        @NotNull
        private Long studentId;
        
        @NotBlank
        private String status; // PRESENT, ABSENT, LATE
        
        private String note;
    }
}
