package fit.tedu.HeThong.service;

import fit.tedu.HeThong.dto.request.ScheduleRequest;
import fit.tedu.HeThong.dto.response.ScheduleResponse;
import fit.tedu.HeThong.entity.ClassRoom;
import fit.tedu.HeThong.entity.Schedule;
import fit.tedu.HeThong.entity.Teacher;
import fit.tedu.HeThong.repository.ClassRoomRepository;
import fit.tedu.HeThong.repository.ScheduleRepository;
import fit.tedu.HeThong.repository.TeacherRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalTime;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class ScheduleService {

    private final ScheduleRepository scheduleRepository;
    private final ClassRoomRepository classRoomRepository;
    private final TeacherRepository teacherRepository;
    private final fit.tedu.HeThong.repository.StudentRepository studentRepository;

    public List<ScheduleResponse> getMySchedule(String username) {
        // Try to find student first
        java.util.Optional<fit.tedu.HeThong.entity.Student> student = studentRepository.findByUsername(username);
        if (student.isPresent() && student.get().getClassRoom() != null) {
            return getSchedulesByClass(student.get().getClassRoom().getId());
        }

        // Try to find teacher
        java.util.Optional<fit.tedu.HeThong.entity.Teacher> teacher = teacherRepository.findByUsername(username);
        if (teacher.isPresent()) {
            return getSchedulesByTeacher(teacher.get().getId());
        }

        throw new RuntimeException("Không tìm thấy thông tin lịch học");
    }

    public List<ScheduleResponse> getSchedulesByClass(Long classId) {
        return scheduleRepository.findByClassRoomIdOrderByDayOfWeekAscStartTimeAsc(classId)
                .stream().filter(schedule -> !schedule.getClassRoom().isArchived())
                .map(this::toResponse)
                .collect(Collectors.toList());
    }

    public List<ScheduleResponse> getSchedulesByClassForTeacher(Long classId, String username) {
        boolean assigned = teacherRepository.findByUsername(username)
                .map(teacher -> classRoomRepository.findById(classId)
                        .map(cls -> cls.getTeacher() != null && cls.getTeacher().getId().equals(teacher.getId()))
                        .orElse(false))
                .orElse(false);
        if (!assigned) {
            throw new org.springframework.security.access.AccessDeniedException("Giáo viên không phụ trách lớp này");
        }
        return getSchedulesByClass(classId);
    }

    public List<ScheduleResponse> getSchedulesByTeacher(Long teacherId) {
        return scheduleRepository.findByTeacherIdOrderByDayOfWeekAscStartTimeAsc(teacherId)
                .stream().filter(schedule -> !schedule.getClassRoom().isArchived())
                .map(this::toResponse)
                .collect(Collectors.toList());
    }

    public List<ScheduleResponse> getSchedulesByTeacherForUser(Long teacherId, String username) {
        Long currentTeacherId = teacherRepository.findByUsername(username)
                .map(Teacher::getId)
                .orElseThrow(() -> new org.springframework.security.access.AccessDeniedException(
                        "Tài khoản không phải giáo viên"));
        if (!currentTeacherId.equals(teacherId)) {
            throw new org.springframework.security.access.AccessDeniedException(
                    "Giáo viên chỉ được xem thời khóa biểu của mình");
        }
        return getSchedulesByTeacher(teacherId);
    }

    public ScheduleResponse getById(Long id) {
        Schedule schedule = scheduleRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy lịch học"));
        return toResponse(schedule);
    }

    public ScheduleResponse getByIdForTeacher(Long id, String username) {
        ScheduleResponse response = getById(id);
        getSchedulesByClassForTeacher(response.getClassId(), username);
        return response;
    }

    @Transactional
    public ScheduleResponse create(ScheduleRequest request) {
        ClassRoom classRoom = classRoomRepository.findById(request.getClassId())
                .orElseThrow(() -> new RuntimeException("Không tìm thấy lớp học"));
        ensureEditable(classRoom);

        Teacher teacher = null;
        if (request.getTeacherId() != null) {
            teacher = teacherRepository.findById(request.getTeacherId())
                    .orElseThrow(() -> new RuntimeException("Không tìm thấy giáo viên"));
        }

        Schedule schedule = Schedule.builder()
                .classRoom(classRoom)
                .subject(request.getSubject())
                .dayOfWeek(request.getDayOfWeek())
                .startTime(LocalTime.parse(request.getStartTime()))
                .endTime(LocalTime.parse(request.getEndTime()))
                .room(request.getRoom())
                .teacher(teacher)
                .build();

        schedule = scheduleRepository.save(schedule);
        return toResponse(schedule);
    }

    @Transactional
    public ScheduleResponse update(Long id, ScheduleRequest request) {
        Schedule schedule = scheduleRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy lịch học"));

        ClassRoom classRoom = classRoomRepository.findById(request.getClassId())
                .orElseThrow(() -> new RuntimeException("Không tìm thấy lớp học"));
        ensureEditable(classRoom);

        Teacher teacher = null;
        if (request.getTeacherId() != null) {
            teacher = teacherRepository.findById(request.getTeacherId())
                    .orElseThrow(() -> new RuntimeException("Không tìm thấy giáo viên"));
        }

        schedule.setClassRoom(classRoom);
        schedule.setSubject(request.getSubject());
        schedule.setDayOfWeek(request.getDayOfWeek());
        schedule.setStartTime(LocalTime.parse(request.getStartTime()));
        schedule.setEndTime(LocalTime.parse(request.getEndTime()));
        schedule.setRoom(request.getRoom());
        schedule.setTeacher(teacher);

        schedule = scheduleRepository.save(schedule);
        return toResponse(schedule);
    }

    @Transactional
    public void delete(Long id) {
        if (!scheduleRepository.existsById(id)) {
            throw new RuntimeException("Không tìm thấy lịch học");
        }
        scheduleRepository.findById(id).ifPresent(schedule -> ensureEditable(schedule.getClassRoom()));
        scheduleRepository.deleteById(id);
    }

    private void ensureEditable(ClassRoom classRoom) {
        if (classRoom.isArchived()) {
            throw new IllegalStateException("Lớp đã lưu trữ, không thể thay đổi thời khóa biểu");
        }
    }

    private ScheduleResponse toResponse(Schedule schedule) {
        return ScheduleResponse.builder()
                .id(schedule.getId())
                .classId(schedule.getClassRoom().getId())
                .className(schedule.getClassRoom().getClassName())
                .subject(schedule.getSubject())
                .dayOfWeek(schedule.getDayOfWeek())
                .dayOfWeekText(getDayOfWeekText(schedule.getDayOfWeek()))
                .startTime(schedule.getStartTime().toString())
                .endTime(schedule.getEndTime().toString())
                .room(schedule.getRoom())
                .teacherId(schedule.getTeacher() != null ? schedule.getTeacher().getId() : null)
                .teacherName(schedule.getTeacher() != null ? schedule.getTeacher().getFullName() : null)
                .build();
    }

    private String getDayOfWeekText(Integer day) {
        return switch (day) {
            case 1 -> "Chủ nhật";
            case 2 -> "Thứ 2";
            case 3 -> "Thứ 3";
            case 4 -> "Thứ 4";
            case 5 -> "Thứ 5";
            case 6 -> "Thứ 6";
            case 7 -> "Thứ 7";
            default -> "";
        };
    }
}
