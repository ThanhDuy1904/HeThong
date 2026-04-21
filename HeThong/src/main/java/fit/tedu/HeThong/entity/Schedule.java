package fit.tedu.HeThong.entity;

import jakarta.persistence.*;
import lombok.*;
import java.time.LocalTime;

@Entity
@Table(name = "schedules")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Schedule {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "class_id", nullable = false)
    private ClassRoom classRoom;

    @Column(nullable = false)
    private String subject; // Môn học

    @Column(name = "day_of_week", nullable = false)
    private Integer dayOfWeek; // 2 = Thứ 2, 3 = Thứ 3, ..., 7 = Thứ 7, 1 = Chủ nhật

    @Column(name = "start_time", nullable = false)
    private LocalTime startTime; // Giờ bắt đầu

    @Column(name = "end_time", nullable = false)
    private LocalTime endTime; // Giờ kết thúc

    @Column(length = 100)
    private String room; // Phòng học

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "teacher_id")
    private Teacher teacher; // Giáo viên dạy môn này
}
