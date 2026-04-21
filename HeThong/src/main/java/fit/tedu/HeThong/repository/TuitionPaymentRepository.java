package fit.tedu.HeThong.repository;

import fit.tedu.HeThong.entity.TuitionPayment;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface TuitionPaymentRepository extends JpaRepository<TuitionPayment, Long> {
    List<TuitionPayment> findByStudentIdOrderByCreatedAtDesc(Long studentId);

    List<TuitionPayment> findByStudentClassRoomIdOrderByCreatedAtDesc(Long classId);
}