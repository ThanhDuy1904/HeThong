package fit.tedu.HeThong.repository;

import fit.tedu.HeThong.entity.Schedule;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ScheduleRepository extends JpaRepository<Schedule, Long> {
    
    List<Schedule> findByClassRoomIdOrderByDayOfWeekAscStartTimeAsc(Long classRoomId);
    
    List<Schedule> findByTeacherIdOrderByDayOfWeekAscStartTimeAsc(Long teacherId);
}
