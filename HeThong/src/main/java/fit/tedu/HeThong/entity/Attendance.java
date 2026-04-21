package fit.tedu.HeThong.entity;

import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(name = "attendances")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Attendance {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "student_id", nullable = false)
    private Student student;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "schedule_id", nullable = false)
    private Schedule schedule;

    @Column(name = "attendance_date", nullable = false)
    private LocalDate attendanceDate; // Ngày điểm danh

    @Column(nullable = false, length = 20)
    private String status; // PRESENT (Có mặt), ABSENT (Vắng), LATE (Đi muộn)

    @Column(length = 500)
    private String note; // Ghi chú

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "marked_by")
    private Teacher markedBy; // Giáo viên điểm danh

    @Column(name = "created_at")
    @Builder.Default
    private LocalDateTime createdAt = LocalDateTime.now();
}
