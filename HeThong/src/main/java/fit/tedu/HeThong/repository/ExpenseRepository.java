package fit.tedu.HeThong.repository;

import fit.tedu.HeThong.entity.Expense;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface ExpenseRepository extends JpaRepository<Expense, Long> {
    List<Expense> findAllByOrderByExpenseYearDescExpenseMonthDescCreatedAtDesc();
}
