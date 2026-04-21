package fit.tedu.HeThong.entity;

import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(name = "teachers")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Teacher {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", unique = true)
    private User user;

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

    @Column(length = 100)
    private String subject;

    @Column(name = "created_at")
    private LocalDateTime createdAt = LocalDateTime.now();
}
