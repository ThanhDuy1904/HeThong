package fit.tedu.HeThong.entity;

import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDate;

@Entity
@Table(name = "teaching_sessions", uniqueConstraints = @UniqueConstraint(columnNames = {"schedule_id", "session_date"}))
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class TeachingSession {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "schedule_id", nullable = false)
    private Schedule schedule;
    @Column(name = "session_date", nullable = false)
    private LocalDate sessionDate;
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    @Builder.Default
    private SessionStatus status = SessionStatus.SCHEDULED;
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "substitute_teacher_id")
    private Teacher substituteTeacher;
    @Column(length = 255)
    private String note;
    public enum SessionStatus { SCHEDULED, TAUGHT, NOT_TAUGHT }
}
