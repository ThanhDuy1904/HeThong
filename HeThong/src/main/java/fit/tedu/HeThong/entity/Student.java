package fit.tedu.HeThong.entity;

import jakarta.persistence.*;
import lombok.*;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(name = "students")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Student {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", unique = true)
    private User user;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "class_id")
    private ClassRoom classRoom;

    @Column(name = "full_name", nullable = false, length = 150)
    private String fullName;

    @Column
    private LocalDate dob;

    @Column(length = 10)
    private String gender;

    @Column(length = 255)
    private String address;

    @Column(length = 20)
    private String phone;

    @Column(name = "parent_name", length = 150)
    private String parentName;

    @Column(name = "parent_phone", length = 20)
    private String parentPhone;

    @Column(length = 30)
    private String status = "ACTIVE";

    @Column(name = "tuition_paid_amount", precision = 15, scale = 2)
    private BigDecimal tuitionPaidAmount;

    @Column(name = "tuition_paid_full", nullable = false)
    private boolean tuitionPaidFull = false;

    @Column(name = "created_at")
    private LocalDateTime createdAt = LocalDateTime.now();
}
