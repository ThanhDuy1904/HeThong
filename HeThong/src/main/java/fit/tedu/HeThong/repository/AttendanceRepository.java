package fit.tedu.HeThong.repository;

import fit.tedu.HeThong.entity.Attendance;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

@Repository
public interface AttendanceRepository extends JpaRepository<Attendance, Long> {
    
    List<Attendance> findByScheduleIdAndAttendanceDate(Long scheduleId, LocalDate date);
    
    Optional<Attendance> findByStudentIdAndScheduleIdAndAttendanceDate(
            Long studentId, Long scheduleId, LocalDate date);
    
    List<Attendance> findByStudentIdOrderByAttendanceDateDesc(Long studentId);
}
