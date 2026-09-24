package fit.tedu.HeThong.repository;

import fit.tedu.HeThong.entity.ArchiveFile;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
public interface ArchiveFileRepository extends JpaRepository<ArchiveFile, Long> {
    List<ArchiveFile> findByUploadedByOrderByCreatedAtDesc(String uploadedBy);
    List<ArchiveFile> findByUploadedByOrSharedWithAdminTrueOrderByCreatedAtDesc(String uploadedBy);
}
