package fit.tedu.HeThong.service;

import fit.tedu.HeThong.dto.request.TeachingSessionRequest;
import fit.tedu.HeThong.dto.response.TeachingSessionResponse;
import fit.tedu.HeThong.entity.*;
import fit.tedu.HeThong.repository.*;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.time.*;
import java.util.*;
import java.util.stream.Collectors;

@Service @RequiredArgsConstructor
public class TeachingSessionService {
    private final ScheduleRepository scheduleRepository;
    private final TeachingSessionRepository sessionRepository;
    private final TeacherRepository teacherRepository;

    @Transactional
    public List<TeachingSessionResponse> week(LocalDate from, LocalDate to) {
        Map<Long, TeachingSession> existing = sessionRepository.findWeek(from, to).stream()
                .collect(Collectors.toMap(s -> key(s.getSchedule().getId(), s.getSessionDate()), s -> s,
                        (first, ignored) -> first, LinkedHashMap::new));
        List<TeachingSession> missing = new ArrayList<>();
        for (Schedule schedule : scheduleRepository.findAll()) {
            LocalDate date = from;
            while (!date.isAfter(to)) {
                if (matches(schedule.getDayOfWeek(), date)) {
                    long key = key(schedule.getId(), date);
                    if (!existing.containsKey(key)) {
                        TeachingSession s = TeachingSession.builder().schedule(schedule).sessionDate(date).build();
                        missing.add(s); existing.put(key, s);
                    }
                }
                date = date.plusDays(1);
            }
        }
        if (!missing.isEmpty()) sessionRepository.saveAll(missing);
        return existing.values().stream().filter(s -> !s.getSessionDate().isBefore(from) && !s.getSessionDate().isAfter(to))
                .sorted(Comparator.comparing(TeachingSession::getSessionDate)
                        .thenComparing(s -> s.getSchedule().getStartTime(),
                                Comparator.nullsLast(Comparator.naturalOrder())))
                .map(this::response).collect(Collectors.toList());
    }
    @Transactional
    public TeachingSessionResponse update(Long id, TeachingSessionRequest request) {
        TeachingSession s = sessionRepository.findById(id).orElseThrow(() -> new RuntimeException("Không tìm thấy buổi dạy"));
        try { s.setStatus(TeachingSession.SessionStatus.valueOf(request.getStatus().toUpperCase())); }
        catch (Exception e) { throw new RuntimeException("Trạng thái buổi dạy không hợp lệ"); }
        s.setNote(request.getNote());
        s.setSubstituteTeacher(request.getSubstituteTeacherId() == null ? null :
                teacherRepository.findById(request.getSubstituteTeacherId()).orElseThrow(() -> new RuntimeException("Không tìm thấy giáo viên thay")));
        return response(sessionRepository.save(s));
    }
    public Map<String, Object> stats(LocalDate from, LocalDate to) {
        if (from == null || to == null || to.isBefore(from)) {
            throw new IllegalArgumentException("Khoảng thời gian không hợp lệ");
        }
        List<TeachingSessionResponse> items = week(from, to);
        long taught = items.stream().filter(s -> "TAUGHT".equals(s.getStatus())).count();
        return Map.of("from", from, "to", to, "taught", taught, "total", items.size(), "display", taught + "/" + items.size());
    }
    private boolean matches(Integer scheduleDay, LocalDate date) {
        int appDay = date.getDayOfWeek() == DayOfWeek.SUNDAY ? 1 : date.getDayOfWeek().getValue() + 1;
        return scheduleDay != null && scheduleDay == appDay;
    }
    private long key(Long scheduleId, LocalDate date) { return scheduleId * 100000L + date.toEpochDay(); }
    private TeachingSessionResponse response(TeachingSession s) {
        Schedule sc = s.getSchedule();
        return TeachingSessionResponse.builder().id(s.getId()).scheduleId(sc.getId()).date(s.getSessionDate())
                .classId(sc.getClassRoom().getId()).className(sc.getClassRoom().getClassName()).subject(sc.getSubject())
                .teacherId(sc.getTeacher() == null ? null : sc.getTeacher().getId())
                .teacherName(sc.getTeacher() == null ? null : sc.getTeacher().getFullName())
                .substituteTeacherId(s.getSubstituteTeacher() == null ? null : s.getSubstituteTeacher().getId())
                .substituteTeacherName(s.getSubstituteTeacher() == null ? null : s.getSubstituteTeacher().getFullName())
                .status(s.getStatus().name()).note(s.getNote()).startTime(sc.getStartTime().toString()).endTime(sc.getEndTime().toString()).build();
    }
}
