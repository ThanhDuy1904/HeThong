package fit.tedu.HeThong.service;

import fit.tedu.HeThong.dto.request.TeacherRequest;
import fit.tedu.HeThong.dto.response.TeacherResponse;
import fit.tedu.HeThong.entity.*;
import fit.tedu.HeThong.repository.*;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class TeacherService {

    private final TeacherRepository teacherRepository;
    private final UserRepository userRepository;
    private final RoleRepository roleRepository;
    private final PasswordEncoder passwordEncoder;

    public List<TeacherResponse> getAll() {
        return teacherRepository.findAll().stream().map(this::toResponse).collect(Collectors.toList());
    }

    public TeacherResponse getById(Long id) {
        return toResponse(teacherRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy giáo viên ID: " + id)));
    }

    public TeacherResponse getByUsername(String username) {
        return toResponse(teacherRepository.findByUsername(username)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy thông tin giáo viên!")));
    }

    public List<TeacherResponse> search(String keyword) {
        return teacherRepository.searchByKeyword(keyword).stream().map(this::toResponse).collect(Collectors.toList());
    }

    @Transactional
    public TeacherResponse create(TeacherRequest req) {
        Teacher teacher = new Teacher();
        teacher.setFullName(req.getFullName());
        teacher.setDob(req.getDob());
        teacher.setGender(req.getGender());
        teacher.setAddress(req.getAddress());
        teacher.setPhone(req.getPhone());
        teacher.setSubject(req.getSubject());

        if (req.getUsername() != null && !req.getUsername().isBlank()) {
            if (userRepository.existsByUsername(req.getUsername())) {
                throw new RuntimeException("Tên đăng nhập đã tồn tại!");
            }
            Role role = roleRepository.findByName("TEACHER").orElseThrow();
            User user = User.builder()
                    .username(req.getUsername())
                    .password(passwordEncoder.encode(req.getPassword() != null ? req.getPassword() : "teacher123"))
                    .email(req.getEmail())
                    .fullName(req.getFullName())
                    .role(role)
                    .enabled(true)
                    .build();
            userRepository.save(user);
            teacher.setUser(user);
        }

        return toResponse(teacherRepository.save(teacher));
    }

    @Transactional
    public TeacherResponse update(Long id, TeacherRequest req) {
        Teacher teacher = teacherRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy giáo viên!"));
        teacher.setFullName(req.getFullName());
        teacher.setDob(req.getDob());
        teacher.setGender(req.getGender());
        teacher.setAddress(req.getAddress());
        teacher.setPhone(req.getPhone());
        teacher.setSubject(req.getSubject());

        // Update user account password if provided
        if (teacher.getUser() != null && req.getPassword() != null && !req.getPassword().isBlank()) {
            User user = teacher.getUser();
            user.setPassword(passwordEncoder.encode(req.getPassword()));
            if (req.getEmail() != null) user.setEmail(req.getEmail());
            userRepository.save(user);
        }

        return toResponse(teacherRepository.save(teacher));
    }

    @Transactional
    public void delete(Long id) {
        Teacher teacher = teacherRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy giáo viên!"));
        
        // Delete linked user account if exists
        User user = teacher.getUser();
        teacherRepository.deleteById(id);
        
        if (user != null) {
            userRepository.delete(user);
        }
    }

    public long countAll() { return teacherRepository.count(); }

    private TeacherResponse toResponse(Teacher t) {
        return TeacherResponse.builder()
                .id(t.getId())
                .fullName(t.getFullName())
                .dob(t.getDob())
                .gender(t.getGender())
                .address(t.getAddress())
                .phone(t.getPhone())
                .subject(t.getSubject())
                .userId(t.getUser() != null ? t.getUser().getId() : null)
                .username(t.getUser() != null ? t.getUser().getUsername() : null)
                .createdAt(t.getCreatedAt())
                .build();
    }
}
