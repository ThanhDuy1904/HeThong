package fit.tedu.HeThong.repository;

import fit.tedu.HeThong.entity.TeachingSession;
import org.springframework.data.jpa.repository.*;
import org.springframework.data.repository.query.Param;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

public interface TeachingSessionRepository extends JpaRepository<TeachingSession, Long> {
    @Query("select s from TeachingSession s join fetch s.schedule sc join fetch sc.classRoom where s.sessionDate between :from and :to order by s.sessionDate, sc.startTime")
    List<TeachingSession> findWeek(@Param("from") LocalDate from, @Param("to") LocalDate to);
    Optional<TeachingSession> findByScheduleIdAndSessionDate(Long scheduleId, LocalDate date);
}
