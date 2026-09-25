package fit.tedu.HeThong.entity;

import jakarta.persistence.*;
import lombok.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "classes")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ClassRoom {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "class_name", nullable = false, length = 100)
    private String className;

    @Column(length = 20)
    private String grade;

    @Column(name = "school_year", length = 20)
    private String schoolYear;

    @Column(name = "tuition_fee", precision = 15, scale = 2)
    private BigDecimal tuitionFee;

    @Column(nullable = false)
    @Builder.Default
    private boolean archived = false;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "teacher_id")
    private Teacher teacher;

    @Column(name = "created_at")
    private LocalDateTime createdAt = LocalDateTime.now();
}
