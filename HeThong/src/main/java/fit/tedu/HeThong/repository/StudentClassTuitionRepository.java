package fit.tedu.HeThong.repository;

import fit.tedu.HeThong.entity.StudentClassTuition;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
import java.util.Optional;

public interface StudentClassTuitionRepository extends JpaRepository<StudentClassTuition, Long> {
    Optional<StudentClassTuition> findByStudentIdAndClassRoomId(Long studentId, Long classId);
    List<StudentClassTuition> findByClassRoomId(Long classId);
}
