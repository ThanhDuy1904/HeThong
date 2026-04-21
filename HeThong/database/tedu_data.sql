-- ================================================================
-- DATABASE: tedu_data
-- Student Management System
-- ================================================================

CREATE DATABASE IF NOT EXISTS tedu_data CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
USE tedu_data;

-- ----------------------------------------------------------------
-- Table: roles
-- ----------------------------------------------------------------
CREATE TABLE IF NOT EXISTS roles (
    id      BIGINT AUTO_INCREMENT PRIMARY KEY,
    name    VARCHAR(50) NOT NULL UNIQUE  -- ADMIN, TEACHER, STUDENT
);

-- ----------------------------------------------------------------
-- Table: users (accounts for login)
-- ----------------------------------------------------------------
CREATE TABLE IF NOT EXISTS users (
    id          BIGINT AUTO_INCREMENT PRIMARY KEY,
    username    VARCHAR(100) NOT NULL UNIQUE,
    password    VARCHAR(255) NOT NULL,
    email       VARCHAR(150) UNIQUE,
    full_name   VARCHAR(150),
    role_id     BIGINT NOT NULL,
    enabled     TINYINT(1) DEFAULT 1,
    created_at  DATETIME DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_user_role FOREIGN KEY (role_id) REFERENCES roles(id)
);

-- ----------------------------------------------------------------
-- Table: classes
-- ----------------------------------------------------------------
CREATE TABLE IF NOT EXISTS classes (
    id          BIGINT AUTO_INCREMENT PRIMARY KEY,
    class_name  VARCHAR(100) NOT NULL,
    grade       VARCHAR(20),
    school_year VARCHAR(20),
    tuition_fee DECIMAL(15,2),
    created_at  DATETIME DEFAULT CURRENT_TIMESTAMP
);

-- ----------------------------------------------------------------
-- Table: teachers
-- ----------------------------------------------------------------
CREATE TABLE IF NOT EXISTS teachers (
    id          BIGINT AUTO_INCREMENT PRIMARY KEY,
    user_id     BIGINT UNIQUE,
    full_name   VARCHAR(150) NOT NULL,
    dob         DATE,
    gender      VARCHAR(10),
    address     VARCHAR(255),
    phone       VARCHAR(20),
    subject     VARCHAR(100),
    created_at  DATETIME DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_teacher_user FOREIGN KEY (user_id) REFERENCES users(id)
);

-- Add homeroom teacher to class
ALTER TABLE classes ADD COLUMN IF NOT EXISTS teacher_id BIGINT,
    ADD CONSTRAINT fk_class_teacher FOREIGN KEY (teacher_id) REFERENCES teachers(id);

-- ----------------------------------------------------------------
-- Table: students
-- ----------------------------------------------------------------
CREATE TABLE IF NOT EXISTS students (
    id          BIGINT AUTO_INCREMENT PRIMARY KEY,
    user_id     BIGINT UNIQUE,
    class_id    BIGINT,
    full_name   VARCHAR(150) NOT NULL,
    dob         DATE,
    gender      VARCHAR(10),
    address     VARCHAR(255),
    phone       VARCHAR(20),
    parent_name VARCHAR(150),
    parent_phone VARCHAR(20),
    status      VARCHAR(30) DEFAULT 'ACTIVE',
    tuition_paid_amount DECIMAL(15,2) DEFAULT 0,
    tuition_paid_full TINYINT(1) DEFAULT 0,
    created_at  DATETIME DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_student_user  FOREIGN KEY (user_id)  REFERENCES users(id),
    CONSTRAINT fk_student_class FOREIGN KEY (class_id) REFERENCES classes(id)
);

-- ----------------------------------------------------------------
-- Table: schedules (Thời khóa biểu)
-- ----------------------------------------------------------------
CREATE TABLE IF NOT EXISTS schedules (
    id          BIGINT AUTO_INCREMENT PRIMARY KEY,
    class_id    BIGINT NOT NULL,
    subject     VARCHAR(100) NOT NULL,
    day_of_week INT NOT NULL,  -- 1=Chủ nhật, 2=Thứ 2, ..., 7=Thứ 7
    start_time  TIME NOT NULL,
    end_time    TIME NOT NULL,
    room        VARCHAR(100),
    teacher_id  BIGINT,
    CONSTRAINT fk_schedule_class FOREIGN KEY (class_id) REFERENCES classes(id),
    CONSTRAINT fk_schedule_teacher FOREIGN KEY (teacher_id) REFERENCES teachers(id)
);

-- ----------------------------------------------------------------
-- Table: attendances (Điểm danh)
-- ----------------------------------------------------------------
CREATE TABLE IF NOT EXISTS attendances (
    id              BIGINT AUTO_INCREMENT PRIMARY KEY,
    student_id      BIGINT NOT NULL,
    schedule_id     BIGINT NOT NULL,
    attendance_date DATE NOT NULL,
    status          VARCHAR(20) NOT NULL,  -- PRESENT, ABSENT, LATE
    note            VARCHAR(500),
    marked_by       BIGINT,
    created_at      DATETIME DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_attendance_student FOREIGN KEY (student_id) REFERENCES students(id),
    CONSTRAINT fk_attendance_schedule FOREIGN KEY (schedule_id) REFERENCES schedules(id),
    CONSTRAINT fk_attendance_teacher FOREIGN KEY (marked_by) REFERENCES teachers(id)
);

-- ----------------------------------------------------------------
-- Table: video_posts
-- ----------------------------------------------------------------
CREATE TABLE IF NOT EXISTS video_posts (
    id            BIGINT AUTO_INCREMENT PRIMARY KEY,
    title         VARCHAR(200) NOT NULL,
    video_url     VARCHAR(500) NOT NULL,
    thumbnail_url VARCHAR(500),
    content       TEXT,
    published     TINYINT(1) DEFAULT 1,
    created_by    BIGINT,
    created_at    DATETIME DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_video_post_user FOREIGN KEY (created_by) REFERENCES users(id)
);

-- ----------------------------------------------------------------
-- Seed data
-- ----------------------------------------------------------------

-- Roles
INSERT IGNORE INTO roles (name) VALUES ('ADMIN'), ('TEACHER'), ('STUDENT'), ('ACCOUNTANT'), ('CONTENT_MANAGER');

-- Admin account  (password: admin123  -> BCrypt hash)
INSERT IGNORE INTO users (username, password, email, full_name, role_id)
VALUES (
    'admin',
    '$2a$10$N9qo8uLOickgx2ZMRZoMyeIjZAgcfl7p92ldGxad68LJZdL17lhWy',
    'admin@tedu.vn',
    'Quản trị viên',
    (SELECT id FROM roles WHERE name = 'ADMIN')
);

-- Sample teacher account  (password: teacher123)
INSERT IGNORE INTO users (username, password, email, full_name, role_id)
VALUES (
    'giaovien01',
    '$2a$10$EblZqNptyYvcLm/VwDptluAkPiRBBdVDwAyauYQ5hkjCY5XRuGJNm',
    'giaovien01@tedu.vn',
    'Nguyễn Văn A',
    (SELECT id FROM roles WHERE name = 'TEACHER')
);

INSERT IGNORE INTO teachers (user_id, full_name, dob, gender, phone, subject)
VALUES (
    (SELECT id FROM users WHERE username = 'giaovien01'),
    'Nguyễn Văn A',
    '1985-06-15',
    'Nam',
    '0901234567',
    'Toán'
);

-- Sample class
INSERT IGNORE INTO classes (class_name, grade, school_year, teacher_id)
VALUES (
    '10A1', '10', '2025-2026',
    (SELECT id FROM teachers WHERE phone = '0901234567')
);

UPDATE classes SET tuition_fee = 2500000 WHERE class_name = '10A1';

-- Sample accountant account (password: teacher123)
INSERT IGNORE INTO users (username, password, email, full_name, role_id)
VALUES (
    'ketoan01',
    '$2a$10$EblZqNptyYvcLm/VwDptluAkPiRBBdVDwAyauYQ5hkjCY5XRuGJNm',
    'ketoan01@tedu.vn',
    'Kế toán TEDU',
    (SELECT id FROM roles WHERE name = 'ACCOUNTANT')
);

-- Sample content manager account (password: teacher123)
INSERT IGNORE INTO users (username, password, email, full_name, role_id)
VALUES (
    'content01',
    '$2a$10$EblZqNptyYvcLm/VwDptluAkPiRBBdVDwAyauYQ5hkjCY5XRuGJNm',
    'content01@tedu.vn',
    'Quản lý nội dung',
    (SELECT id FROM roles WHERE name = 'CONTENT_MANAGER')
);

INSERT IGNORE INTO video_posts (title, video_url, thumbnail_url, content, published, created_by)
VALUES (
    'Giới thiệu chương trình học TEDU',
    'https://www.youtube.com/embed/dQw4w9WgXcQ',
    'https://img.youtube.com/vi/dQw4w9WgXcQ/hqdefault.jpg',
    'Video giới thiệu tổng quan chương trình học dành cho học sinh và phụ huynh.',
    1,
    (SELECT id FROM users WHERE username = 'admin')
);

-- Sample student account  (password: student123)
INSERT IGNORE INTO users (username, password, email, full_name, role_id)
VALUES (
    'hocsinh',
    '$2a$10$Ltj.l1e/Lq7i5TJOL.BFrunlUFrCaVqCLqdS9jDn25u9FiDfKl3Zy',
    'hocsinh@tedu.vn',
    'Thanh Duy1',
    (SELECT id FROM roles WHERE name = 'STUDENT')
);

INSERT IGNORE INTO students (user_id, class_id, full_name, dob, gender, phone, parent_name, parent_phone)
VALUES (
    (SELECT id FROM users WHERE username = 'hocsinh'),
    (SELECT id FROM classes WHERE class_name = '10A1'),
    'Thanh Duy1',
    '2010-03-20',
    'Nam',
    '0912345678',
    'Trần Văn C',
    '0987654321'
);

-- Sample schedules for class 10A1
INSERT IGNORE INTO schedules (class_id, subject, day_of_week, start_time, end_time, room, teacher_id)
VALUES 
    -- Thứ 2
    ((SELECT id FROM classes WHERE class_name = '10A1'), 'Toán', 2, '07:00:00', '08:30:00', 'A101', 
     (SELECT id FROM teachers WHERE phone = '0901234567')),
    ((SELECT id FROM classes WHERE class_name = '10A1'), 'Lý', 2, '08:45:00', '10:15:00', 'A101', NULL),
    ((SELECT id FROM classes WHERE class_name = '10A1'), 'Văn', 2, '13:30:00', '15:00:00', 'A101', NULL),
    ((SELECT id FROM classes WHERE class_name = '10A1'), 'Anh', 2, '15:15:00', '16:45:00', 'B201', NULL),
    
    -- Thứ 3
    ((SELECT id FROM classes WHERE class_name = '10A1'), 'Hóa', 3, '07:00:00', '08:30:00', 'A102', NULL),
    ((SELECT id FROM classes WHERE class_name = '10A1'), 'Sinh', 3, '08:45:00', '10:15:00', 'A102', NULL),
    ((SELECT id FROM classes WHERE class_name = '10A1'), 'GDCD', 3, '13:30:00', '15:00:00', 'A101', NULL),
    ((SELECT id FROM classes WHERE class_name = '10A1'), 'Sử', 3, '15:15:00', '16:45:00', 'A101', NULL),
    
    -- Thứ 4
    ((SELECT id FROM classes WHERE class_name = '10A1'), 'Toán', 4, '07:00:00', '08:30:00', 'A101', 
     (SELECT id FROM teachers WHERE phone = '0901234567')),
    ((SELECT id FROM classes WHERE class_name = '10A1'), 'Văn', 4, '08:45:00', '10:15:00', 'A101', NULL),
    ((SELECT id FROM classes WHERE class_name = '10A1'), 'Địa', 4, '13:30:00', '15:00:00', 'A103', NULL),
    ((SELECT id FROM classes WHERE class_name = '10A1'), 'TD', 4, '15:15:00', '16:45:00', 'Sân', NULL),
    
    -- Thứ 5
    ((SELECT id FROM classes WHERE class_name = '10A1'), 'Anh', 5, '07:00:00', '08:30:00', 'B201', NULL),
    ((SELECT id FROM classes WHERE class_name = '10A1'), 'Lý', 5, '08:45:00', '10:15:00', 'A101', NULL),
    ((SELECT id FROM classes WHERE class_name = '10A1'), 'Hóa', 5, '13:30:00', '15:00:00', 'A102', NULL),
    ((SELECT id FROM classes WHERE class_name = '10A1'), 'Tin', 5, '15:15:00', '16:45:00', 'C301', NULL),
    
    -- Thứ 6
    ((SELECT id FROM classes WHERE class_name = '10A1'), 'Văn', 6, '07:00:00', '08:30:00', 'A101', NULL),
    ((SELECT id FROM classes WHERE class_name = '10A1'), 'Toán', 6, '08:45:00', '10:15:00', 'A101', 
     (SELECT id FROM teachers WHERE phone = '0901234567')),
    ((SELECT id FROM classes WHERE class_name = '10A1'), 'Sinh', 6, '13:30:00', '15:00:00', 'A102', NULL),
    ((SELECT id FROM classes WHERE class_name = '10A1'), 'Nhạc', 6, '15:15:00', '16:45:00', 'D101', NULL);

-- Sample attendances (điểm danh mẫu cho học sinh hocsinh)
INSERT IGNORE INTO attendances (student_id, schedule_id, attendance_date, status, note, marked_by)
SELECT 
    (SELECT id FROM students WHERE full_name = 'Thanh Duy1'),
    s.id,
    '2026-02-24', -- Thứ 2 tuần trước
    'PRESENT',
    NULL,
    (SELECT id FROM teachers WHERE phone = '0901234567')
FROM schedules s 
WHERE s.class_id = (SELECT id FROM classes WHERE class_name = '10A1') 
  AND s.day_of_week = 2
LIMIT 4;

INSERT IGNORE INTO attendances (student_id, schedule_id, attendance_date, status, note, marked_by)
SELECT 
    (SELECT id FROM students WHERE full_name = 'Thanh Duy1'),
    s.id,
    '2026-02-25', -- Thứ 3
    'PRESENT',
    NULL,
    (SELECT id FROM teachers WHERE phone = '0901234567')
FROM schedules s 
WHERE s.class_id = (SELECT id FROM classes WHERE class_name = '10A1') 
  AND s.day_of_week = 3
LIMIT 4;

INSERT IGNORE INTO attendances (student_id, schedule_id, attendance_date, status, note, marked_by)
SELECT 
    (SELECT id FROM students WHERE full_name = 'Thanh Duy1'),
    s.id,
    '2026-02-26', -- Thứ 4
    'LATE',
    'Đi muộn 15 phút',
    (SELECT id FROM teachers WHERE phone = '0901234567')
FROM schedules s 
WHERE s.class_id = (SELECT id FROM classes WHERE class_name = '10A1') 
  AND s.day_of_week = 4
LIMIT 2;
