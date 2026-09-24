package fit.tedu.HeThong.entity;

import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "archive_files")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class ArchiveFile {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @Column(nullable = false, length = 255)
    private String originalName;
    @Column(nullable = false, length = 255)
    private String storedName;
    @Column(nullable = false, length = 120)
    private String contentType;
    @Column(nullable = false)
    private long size;
    @Column(nullable = false, length = 100)
    private String uploadedBy;
    @Builder.Default
    @Column(nullable = false)
    private boolean sharedWithAdmin = false;
    @Column(nullable = false)
    private LocalDateTime createdAt = LocalDateTime.now();
}
