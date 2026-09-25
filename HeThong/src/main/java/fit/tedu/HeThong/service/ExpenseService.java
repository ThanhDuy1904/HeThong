package fit.tedu.HeThong.service;

import fit.tedu.HeThong.dto.response.*;
import fit.tedu.HeThong.entity.*;
import fit.tedu.HeThong.repository.*;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import java.math.BigDecimal;
import java.nio.file.*;
import java.time.LocalDateTime;
import java.util.*;

@Service
@RequiredArgsConstructor
public class ExpenseService {
    private final ExpenseRepository repository;
    private final UserRepository userRepository;
    private final TuitionPaymentRepository paymentRepository;
    @Value("${app.archive.dir:./data/archive}") private String directory;

    public List<ExpenseResponse> list() {
        return repository.findAllByOrderByExpenseYearDescExpenseMonthDescCreatedAtDesc()
                .stream().map(this::response).toList();
    }

    public BigDecimal availableAmount() {
        return paymentRepository.findAll().stream().map(TuitionPayment::getAmount)
                .filter(Objects::nonNull).reduce(BigDecimal.ZERO, BigDecimal::add)
                .subtract(repository.findAll().stream().map(Expense::getAmount)
                        .filter(Objects::nonNull).reduce(BigDecimal.ZERO, BigDecimal::add))
                .max(BigDecimal.ZERO);
    }

    public ExpenseResponse create(String category, String description, int year, int month,
                                  BigDecimal amount, MultipartFile image, String username) {
        if (category == null || category.isBlank()) throw new IllegalArgumentException("Vui lòng chọn loại chi tiêu");
        if (year < 2000 || month < 1 || month > 12) throw new IllegalArgumentException("Thời gian chi tiêu không hợp lệ");
        if (amount == null || amount.compareTo(BigDecimal.ZERO) <= 0) throw new IllegalArgumentException("Số tiền chi phải lớn hơn 0");
        if (amount.compareTo(availableAmount()) > 0) throw new IllegalArgumentException("Số tiền chi vượt quá số tiền khả dụng");
        Expense expense = Expense.builder().category(category).description(description).expenseYear(year)
                .expenseMonth(month).amount(amount)
                .createdBy(userRepository.findByUsername(username).orElse(null)).build();
        if (image != null && !image.isEmpty()) {
            try {
                Path dir = Paths.get(directory).toAbsolutePath().normalize();
                Files.createDirectories(dir);
                String original = image.getOriginalFilename() == null ? "anh" : image.getOriginalFilename();
                String stored = UUID.randomUUID() + "_" + original.replaceAll("[^\\p{L}\\p{N}._-]", "_");
                Files.copy(image.getInputStream(), dir.resolve(stored), StandardCopyOption.REPLACE_EXISTING);
                expense.setImageName(original);
                expense.setImageStoredName(stored);
                expense.setImageContentType(image.getContentType());
            } catch (Exception e) {
                throw new RuntimeException("Không thể lưu hình ảnh phiếu chi", e);
            }
        }
        return response(repository.save(expense));
    }

    public Expense get(Long id) { return repository.findById(id).orElseThrow(() -> new RuntimeException("Không tìm thấy phiếu chi")); }
    public void delete(Long id) {
        Expense expense = get(id);
        if (expense.getImageStoredName() != null) {
            try { Files.deleteIfExists(Paths.get(directory).toAbsolutePath().resolve(expense.getImageStoredName())); }
            catch (Exception e) { throw new RuntimeException("Không thể xóa hình ảnh phiếu chi", e); }
        }
        repository.delete(expense);
    }
    public Path imagePath(Long id) {
        Expense expense = get(id);
        if (expense.getImageStoredName() == null) throw new RuntimeException("Phiếu chi không có hình ảnh");
        return Paths.get(directory).toAbsolutePath().resolve(expense.getImageStoredName());
    }
    public String imageType(Long id) { return get(id).getImageContentType(); }

    private ExpenseResponse response(Expense e) {
        return ExpenseResponse.builder().id(e.getId()).category(e.getCategory()).description(e.getDescription())
                .year(e.getExpenseYear()).month(e.getExpenseMonth()).amount(e.getAmount())
                .imageName(e.getImageName()).createdBy(e.getCreatedBy() == null ? null : e.getCreatedBy().getFullName())
                .createdAt(e.getCreatedAt()).build();
    }
}
