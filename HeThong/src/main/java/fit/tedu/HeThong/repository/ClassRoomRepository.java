package fit.tedu.HeThong.repository;

import fit.tedu.HeThong.entity.ClassRoom;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface ClassRoomRepository extends JpaRepository<ClassRoom, Long> {
    List<ClassRoom> findByTeacherId(Long teacherId);
    List<ClassRoom> findBySchoolYear(String schoolYear);
}
