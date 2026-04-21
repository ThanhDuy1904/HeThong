package fit.tedu.HeThong.repository;

import fit.tedu.HeThong.entity.Student;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import java.util.List;
import java.util.Optional;

public interface StudentRepository extends JpaRepository<Student, Long> {
    Optional<Student> findByUserId(Long userId);
    
    @Query("SELECT s FROM Student s WHERE s.user.username = :username")
    Optional<Student> findByUsername(@Param("username") String username);
    
    List<Student> findByClassRoomId(Long classId);

    @Query("SELECT s FROM Student s WHERE " +
           "LOWER(s.fullName) LIKE LOWER(CONCAT('%', :keyword, '%')) OR " +
           "LOWER(s.phone) LIKE LOWER(CONCAT('%', :keyword, '%'))")
    List<Student> searchByKeyword(@Param("keyword") String keyword);

    long countByStatus(String status);
}
