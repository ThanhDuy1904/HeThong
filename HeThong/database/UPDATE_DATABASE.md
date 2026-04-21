# Cập nhật Database

## Cách 1: Chạy lại toàn bộ script SQL

```bash
# Kết nối vào MySQL
mysql -u root -p

# Xóa database cũ và tạo lại (CẨN THẬN: Sẽ mất hết dữ liệu!)
DROP DATABASE IF EXISTS tedu_data;

# Chạy script SQL
source C:/Users/THANH NGA/Downloads/HeThong/HeThong/database/tedu_data.sql

# Hoặc từ PowerShell:
Get-Content "C:\Users\THANH NGA\Downloads\HeThong\HeThong\database\tedu_data.sql" | mysql -u root -p
```

## Cách 2: Chỉ thêm các bảng mới (giữ dữ liệu cũ)

```sql
USE tedu_data;

-- Tạo bảng schedules
CREATE TABLE IF NOT EXISTS schedules (
    id          BIGINT AUTO_INCREMENT PRIMARY KEY,
    class_id    BIGINT NOT NULL,
    subject     VARCHAR(100) NOT NULL,
    day_of_week INT NOT NULL,
    start_time  TIME NOT NULL,
    end_time    TIME NOT NULL,
    room        VARCHAR(100),
    teacher_id  BIGINT,
    CONSTRAINT fk_schedule_class FOREIGN KEY (class_id) REFERENCES classes(id),
    CONSTRAINT fk_schedule_teacher FOREIGN KEY (teacher_id) REFERENCES teachers(id)
);

-- Tạo bảng attendances
CREATE TABLE IF NOT EXISTS attendances (
    id              BIGINT AUTO_INCREMENT PRIMARY KEY,
    student_id      BIGINT NOT NULL,
    schedule_id     BIGINT NOT NULL,
    attendance_date DATE NOT NULL,
    status          VARCHAR(20) NOT NULL,
    note            VARCHAR(500),
    marked_by       BIGINT,
    created_at      DATETIME DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_attendance_student FOREIGN KEY (student_id) REFERENCES students(id),
    CONSTRAINT fk_attendance_schedule FOREIGN KEY (schedule_id) REFERENCES schedules(id),
    CONSTRAINT fk_attendance_teacher FOREIGN KEY (marked_by) REFERENCES teachers(id)
);

-- Sau đó chạy các INSERT để thêm dữ liệu mẫu (copy từ file tedu_data.sql)
```

## Tài khoản đăng nhập

Sau khi chạy xong:

- **Admin**: username=`admin`, password=`admin123`
- **Giáo viên**: username=`giaovien01`, password=`teacher123`
- **Học sinh**: username=`hocsinh`, password=`student123`

## Sau khi cập nhật database

1. Restart Spring Boot server
2. Refresh trình duyệt (F5)
3. Đăng nhập với tài khoản học sinh `hocsinh` / `student123`
4. Vào trang "Thời khóa biểu" để xem lịch học
