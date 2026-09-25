package fit.tedu.HeThong.service;

import fit.tedu.HeThong.dto.response.*;
import fit.tedu.HeThong.entity.*;
import fit.tedu.HeThong.repository.*;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class RevenueService {
    private final ClassRoomRepository classRoomRepository;
    private final StudentRepository studentRepository;
    private final StudentClassTuitionRepository classTuitionRepository;
    private final TuitionPaymentRepository paymentRepository;
    private final ExpenseRepository expenseRepository;

    public RevenueReportResponse report(String schoolYear, Integer year, Integer month) {
        int selectedYear = year == null ? LocalDate.now().getYear() : year;
        String selectedSchoolYear = schoolYear;
        if (selectedSchoolYear == null || selectedSchoolYear.isBlank()) {
            selectedSchoolYear = selectedYear + "-" + (selectedYear + 1);
        }
        final String schoolYearFilter = selectedSchoolYear;
        List<RevenueClassResponse> byClass = new ArrayList<>();
        for (ClassRoom cls : classRoomRepository.findAll()) {
            if (cls.isArchived() || !selectedSchoolYear.equalsIgnoreCase(
                    cls.getSchoolYear() == null ? "" : cls.getSchoolYear().trim())) continue;
            BigDecimal fee = nz(cls.getTuitionFee());
            BigDecimal due = BigDecimal.ZERO;
            BigDecimal collected = BigDecimal.ZERO;
            for (Student student : studentRepository.findByAnyClassId(cls.getId())) {
                due = due.add(fee);
                collected = collected.add(classTuitionRepository
                        .findByStudentIdAndClassRoomId(student.getId(), cls.getId())
                        .map(StudentClassTuition::getPaidAmount).orElse(
                                student.getClassRoom() != null && student.getClassRoom().getId().equals(cls.getId())
                                        ? nz(student.getTuitionPaidAmount()) : BigDecimal.ZERO));
            }
            BigDecimal remaining = due.subtract(collected).max(BigDecimal.ZERO);
            byClass.add(RevenueClassResponse.builder().className(cls.getClassName())
                    .schoolYear(cls.getSchoolYear()).totalDue(due).collected(collected)
                    .remaining(remaining).uncollected(remaining).build());
        }
        Set<Long> classIds = classRoomRepository.findAll().stream()
                .filter(c -> !c.isArchived() && schoolYearFilter.equalsIgnoreCase(
                        c.getSchoolYear() == null ? "" : c.getSchoolYear().trim()))
                .map(ClassRoom::getId).collect(Collectors.toSet());
        List<TuitionPayment> payments = paymentRepository.findAll();
        BigDecimal collected = payments.stream().filter(p -> inScope(p, classIds, selectedYear, month))
                .map(TuitionPayment::getAmount).reduce(BigDecimal.ZERO, BigDecimal::add);
        List<RevenueMonthResponse> monthly = new ArrayList<>();
        for (int m = 1; m <= 12; m++) {
            final int currentMonth = m;
            BigDecimal value = payments.stream().filter(p -> inScope(p, classIds, selectedYear, currentMonth))
                    .map(TuitionPayment::getAmount).reduce(BigDecimal.ZERO, BigDecimal::add);
            monthly.add(new RevenueMonthResponse(m, value));
        }
        BigDecimal due = byClass.stream().map(RevenueClassResponse::getTotalDue).reduce(BigDecimal.ZERO, BigDecimal::add);
        BigDecimal remaining = byClass.stream().map(RevenueClassResponse::getRemaining).reduce(BigDecimal.ZERO, BigDecimal::add);
        BigDecimal expenses = expenseRepository.findAll().stream()
                .filter(e -> e.getExpenseYear() == selectedYear
                        && (month == null || e.getExpenseMonth() == month))
                .map(Expense::getAmount).filter(Objects::nonNull)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        return RevenueReportResponse.builder().totalCollected(collected).totalDue(due)
                .totalRemaining(remaining.subtract(expenses).max(BigDecimal.ZERO))
                .totalUncollected(remaining).totalExpense(expenses)
                .byClass(byClass).monthly(monthly).build();
    }

    private boolean inScope(TuitionPayment payment, Set<Long> classIds, int year, Integer month) {
        if (payment.getCreatedAt() == null || payment.getCreatedAt().getYear() != year
                || (month != null && payment.getCreatedAt().getMonthValue() != month)) return false;
        Student student = payment.getStudent();
        return student.getClasses().stream().anyMatch(c -> classIds.contains(c.getId()))
                || (student.getClassRoom() != null && classIds.contains(student.getClassRoom().getId()));
    }

    private BigDecimal nz(BigDecimal value) { return value == null ? BigDecimal.ZERO : value; }
}
