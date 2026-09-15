package fit.tedu.HeThong.service;

import fit.tedu.HeThong.dto.request.AttendanceRequest;
import fit.tedu.HeThong.dto.response.AttendanceResponse;
import fit.tedu.HeThong.entity.Attendance;
import fit.tedu.HeThong.entity.Schedule;
import fit.tedu.HeThong.entity.Student;
import fit.tedu.HeThong.entity.Teacher;
import fit.tedu.HeThong.repository.AttendanceRepository;
import fit.tedu.HeThong.repository.ScheduleRepository;
import fit.tedu.HeThong.repository.StudentRepository;
import fit.tedu.HeThong.repository.TeacherRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class AttendanceService {

    private final AttendanceRepository attendanceRepository;
    private final ScheduleRepository scheduleRepository;
    private final StudentRepository studentRepository;
    private final TeacherRepository teacherRepository;

    public List<AttendanceResponse> getAttendancesByScheduleAndDate(Long scheduleId, String dateStr) {
        return getAttendancesByScheduleAndDate(scheduleId, dateStr, null);
    }

    public List<AttendanceResponse> getAttendancesByScheduleAndDate(Long scheduleId, String dateStr, Long teacherId) {
        Schedule schedule = scheduleRepository.findById(scheduleId)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy lịch học"));
        if (teacherId != null && (schedule.getClassRoom().getTeacher() == null
                || !schedule.getClassRoom().getTeacher().getId().equals(teacherId))) {
            throw new org.springframework.security.access.AccessDeniedException(
                    "Giáo viên chỉ được xem điểm danh lớp mình phụ trách");
        }
        LocalDate date = LocalDate.parse(dateStr);
        return attendanceRepository.findByScheduleIdAndAttendanceDate(scheduleId, date)
                .stream()
                .map(this::toResponse)
                .collect(Collectors.toList());
    }

    public List<AttendanceResponse> getAttendancesByStudent(Long studentId) {
        return attendanceRepository.findByStudentIdOrderByAttendanceDateDesc(studentId)
                .stream()
                .map(this::toResponse)
                .collect(Collectors.toList());
    }

    public List<AttendanceResponse> getAttendancesByStudentForTeacher(Long studentId, String username) {
        Student student = studentRepository.findById(studentId)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy học sinh"));
        Long teacherId = teacherRepository.findByUsername(username)
                .map(Teacher::getId)
                .orElseThrow(() -> new org.springframework.security.access.AccessDeniedException(
                        "Tài khoản không phải giáo viên"));
        boolean assigned = student.getClasses().stream()
                .anyMatch(cls -> cls.getTeacher() != null && cls.getTeacher().getId().equals(teacherId));
        if (!assigned && (student.getClassRoom() == null || student.getClassRoom().getTeacher() == null
                || !student.getClassRoom().getTeacher().getId().equals(teacherId))) {
            throw new org.springframework.security.access.AccessDeniedException(
                    "Giáo viên không phụ trách học sinh này");
        }
        return getAttendancesByStudent(studentId);
    }

    @Transactional
    public List<AttendanceResponse> markAttendance(AttendanceRequest request, Long teacherId) {
        Schedule schedule = scheduleRepository.findById(request.getScheduleId())
                .orElseThrow(() -> new RuntimeException("Không tìm thấy lịch học"));

        Teacher teacher = null;
        if (teacherId != null) {
            teacher = teacherRepository.findById(teacherId)
                    .orElse(null); // Không throw exception nếu không tìm thấy
            if (teacher == null || schedule.getClassRoom().getTeacher() == null
                    || !schedule.getClassRoom().getTeacher().getId().equals(teacherId)) {
                throw new org.springframework.security.access.AccessDeniedException(
                        "Giáo viên chỉ được điểm danh lớp mình phụ trách");
            }
        }

        LocalDate attendanceDate = LocalDate.parse(request.getAttendanceDate());
        int scheduleDay = attendanceDate.getDayOfWeek().getValue() == 7
                ? 1
                : attendanceDate.getDayOfWeek().getValue() + 1;
        if (schedule.getDayOfWeek() != scheduleDay) {
            throw new RuntimeException("Ngày điểm danh không khớp với thứ của buổi học");
        }
        List<AttendanceResponse> responses = new ArrayList<>();

        for (AttendanceRequest.StudentAttendance sa : request.getAttendances()) {
            Student student = studentRepository.findById(sa.getStudentId())
                    .orElseThrow(() -> new RuntimeException("Không tìm thấy học sinh ID: " + sa.getStudentId()));

            // Kiểm tra xem đã điểm danh chưa
            Attendance attendance = attendanceRepository
                    .findByStudentIdAndScheduleIdAndAttendanceDate(
                            sa.getStudentId(), request.getScheduleId(), attendanceDate)
                    .orElse(null);

            if (attendance == null) {
                // Tạo mới
                attendance = new Attendance();
            }
            
            // Set tất cả các field (cả mới và update)
            attendance.setStudent(student);
            attendance.setSchedule(schedule);
            attendance.setAttendanceDate(attendanceDate);
            attendance.setStatus(sa.getStatus());
            attendance.setNote(sa.getNote());
            attendance.setMarkedBy(teacher);
            
            if (attendance.getCreatedAt() == null) {
                attendance.setCreatedAt(LocalDateTime.now());
            }

            attendance = attendanceRepository.save(attendance);
            responses.add(toResponse(attendance));
        }

        return responses;
    }

    private AttendanceResponse toResponse(Attendance attendance) {
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd");
        DateTimeFormatter dateTimeFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

        return AttendanceResponse.builder()
                .id(attendance.getId())
                .studentId(attendance.getStudent().getId())
                .studentName(attendance.getStudent().getFullName())
                .scheduleId(attendance.getSchedule().getId())
                .subject(attendance.getSchedule().getSubject())
                .attendanceDate(attendance.getAttendanceDate().format(formatter))
                .status(attendance.getStatus())
                .note(attendance.getNote())
                .markedByName(attendance.getMarkedBy() != null ? attendance.getMarkedBy().getFullName() : null)
                .createdAt(attendance.getCreatedAt().format(dateTimeFormatter))
                .build();
    }
}
