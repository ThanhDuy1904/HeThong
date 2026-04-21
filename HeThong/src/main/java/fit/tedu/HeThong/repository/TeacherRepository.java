package fit.tedu.HeThong.repository;

import fit.tedu.HeThong.entity.Teacher;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import java.util.List;
import java.util.Optional;

public interface TeacherRepository extends JpaRepository<Teacher, Long> {
    Optional<Teacher> findByUserId(Long userId);
    
    @Query("SELECT t FROM Teacher t WHERE t.user.username = :username")
    Optional<Teacher> findByUsername(@Param("username") String username);

    @Query("SELECT t FROM Teacher t WHERE " +
           "LOWER(t.fullName) LIKE LOWER(CONCAT('%', :keyword, '%')) OR " +
           "LOWER(t.subject) LIKE LOWER(CONCAT('%', :keyword, '%'))")
    List<Teacher> searchByKeyword(@Param("keyword") String keyword);
}
