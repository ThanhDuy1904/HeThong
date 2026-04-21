package fit.tedu.HeThong.service;

import fit.tedu.HeThong.dto.request.ClassRequest;
import fit.tedu.HeThong.dto.response.ClassResponse;
import fit.tedu.HeThong.entity.ClassRoom;
import fit.tedu.HeThong.repository.*;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class ClassService {

    private final ClassRoomRepository classRoomRepository;
    private final TeacherRepository teacherRepository;
    private final StudentRepository studentRepository;

    public List<ClassResponse> getAll() {
        return classRoomRepository.findAll().stream().map(this::toResponse).collect(Collectors.toList());
    }

    public ClassResponse getById(Long id) {
        return toResponse(classRoomRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy lớp ID: " + id)));
    }

    @Transactional
    public ClassResponse create(ClassRequest req) {
        ClassRoom cls = ClassRoom.builder()
                .className(req.getClassName())
                .grade(req.getGrade())
                .schoolYear(req.getSchoolYear())
            .tuitionFee(req.getTuitionFee())
                .build();
        if (req.getTeacherId() != null) {
            teacherRepository.findById(req.getTeacherId()).ifPresent(cls::setTeacher);
        }
        return toResponse(classRoomRepository.save(cls));
    }

    @Transactional
    public ClassResponse update(Long id, ClassRequest req) {
        ClassRoom cls = classRoomRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy lớp!"));
        cls.setClassName(req.getClassName());
        cls.setGrade(req.getGrade());
        cls.setSchoolYear(req.getSchoolYear());
        cls.setTuitionFee(req.getTuitionFee());
        if (req.getTeacherId() != null) {
            teacherRepository.findById(req.getTeacherId()).ifPresent(cls::setTeacher);
        } else {
            cls.setTeacher(null);
        }
        return toResponse(classRoomRepository.save(cls));
    }

    @Transactional
    public ClassResponse updateTuitionFee(Long id, BigDecimal tuitionFee) {
        ClassRoom cls = classRoomRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy lớp!"));
        cls.setTuitionFee(tuitionFee);
        return toResponse(classRoomRepository.save(cls));
    }

    @Transactional
    public void delete(Long id) {
        classRoomRepository.deleteById(id);
    }

    public long countAll() { return classRoomRepository.count(); }

    private ClassResponse toResponse(ClassRoom c) {
        long studentCount = studentRepository.findByClassRoomId(c.getId()).size();
        return ClassResponse.builder()
                .id(c.getId())
                .className(c.getClassName())
                .grade(c.getGrade())
                .schoolYear(c.getSchoolYear())
                .tuitionFee(c.getTuitionFee())
                .teacherId(c.getTeacher() != null ? c.getTeacher().getId() : null)
                .teacherName(c.getTeacher() != null ? c.getTeacher().getFullName() : null)
                .studentCount(studentCount)
                .createdAt(c.getCreatedAt())
                .build();
    }
}
