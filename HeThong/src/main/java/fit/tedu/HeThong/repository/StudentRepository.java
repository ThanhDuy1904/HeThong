package fit.tedu.HeThong.repository;

import fit.tedu.HeThong.entity.Student;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
import jakarta.persistence.LockModeType;

public interface StudentRepository extends JpaRepository<Student, Long> {
    Optional<Student> findByUserId(Long userId);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT s FROM Student s WHERE s.id = :id")
    Optional<Student> findByIdForUpdate(@Param("id") Long id);
    
    @Query("SELECT s FROM Student s WHERE s.user.username = :username")
    Optional<Student> findByUsername(@Param("username") String username);
    
    List<Student> findByClassRoomId(Long classId);

    @Query("SELECT DISTINCT s FROM Student s LEFT JOIN s.classes c WHERE s.classRoom.id = :classId OR c.id = :classId")
    List<Student> findByAnyClassId(@Param("classId") Long classId);

    @Query("SELECT COUNT(DISTINCT s) FROM Student s LEFT JOIN s.classes c WHERE s.classRoom.id = :classId OR c.id = :classId")
    long countByAnyClassId(@Param("classId") Long classId);

    long countByTuitionPaidFullTrue();

    long countByTuitionPaidFullFalse();

    @Query("SELECT COALESCE(SUM(CASE WHEN s.classRoom IS NOT NULL AND s.classRoom.tuitionFee IS NOT NULL THEN s.classRoom.tuitionFee ELSE 0 END - CASE WHEN s.tuitionPaidAmount IS NOT NULL THEN s.tuitionPaidAmount ELSE 0 END), 0) FROM Student s")
    BigDecimal sumTuitionRemaining();

    @Query("SELECT COALESCE(SUM(CASE WHEN s.tuitionPaidAmount IS NOT NULL THEN s.tuitionPaidAmount ELSE 0 END), 0) FROM Student s")
    BigDecimal sumTuitionPaid();

    @Query("SELECT s FROM Student s WHERE " +
           "LOWER(s.fullName) LIKE LOWER(CONCAT('%', :keyword, '%')) OR " +
           "LOWER(s.phone) LIKE LOWER(CONCAT('%', :keyword, '%'))")
    List<Student> searchByKeyword(@Param("keyword") String keyword);

    long countByStatus(String status);
}
