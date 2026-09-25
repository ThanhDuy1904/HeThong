package fit.tedu.HeThong.entity;

import jakarta.persistence.*;
import lombok.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "expenses")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Expense {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @Column(nullable = false, length = 40)
    private String category;
    @Column(length = 255)
    private String description;
    @Column(nullable = false)
    private int expenseYear;
    @Column(nullable = false)
    private int expenseMonth;
    @Column(nullable = false, precision = 15, scale = 2)
    private BigDecimal amount;
    @Column(length = 255)
    private String imageName;
    @Column(length = 255)
    private String imageStoredName;
    @Column(length = 120)
    private String imageContentType;
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "created_by")
    private User createdBy;
    @Column(nullable = false)
    @Builder.Default
    private LocalDateTime createdAt = LocalDateTime.now();
}
