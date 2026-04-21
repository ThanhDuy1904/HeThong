package fit.tedu.HeThong.service;

import fit.tedu.HeThong.dto.request.ChangePasswordRequest;
import fit.tedu.HeThong.dto.request.UpdateProfileRequest;
import fit.tedu.HeThong.dto.response.UserProfileResponse;
import fit.tedu.HeThong.entity.Student;
import fit.tedu.HeThong.entity.Teacher;
import fit.tedu.HeThong.entity.User;
import fit.tedu.HeThong.repository.StudentRepository;
import fit.tedu.HeThong.repository.TeacherRepository;
import fit.tedu.HeThong.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;

@Service
@RequiredArgsConstructor
public class UserService {

    private final UserRepository userRepository;
    private final StudentRepository studentRepository;
    private final TeacherRepository teacherRepository;
    private final PasswordEncoder passwordEncoder;

    public UserProfileResponse getProfile(Long userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy người dùng!"));

        UserProfileResponse.UserProfileResponseBuilder builder = UserProfileResponse.builder()
                .id(user.getId())
                .username(user.getUsername())
                .email(user.getEmail())
                .fullName(user.getFullName())
                .role(user.getRole().getName())
                .enabled(user.isEnabled());

        // Load student info if user is a student
        Optional<Student> student = studentRepository.findByUserId(userId);
        if (student.isPresent()) {
            Student s = student.get();
            builder.studentId(s.getId())
                   .dob(s.getDob())
                   .gender(s.getGender())
                   .address(s.getAddress())
                   .phone(s.getPhone())
                   .parentName(s.getParentName())
                   .parentPhone(s.getParentPhone())
                   .status(s.getStatus())
                   .classId(s.getClassRoom() != null ? s.getClassRoom().getId() : null)
                   .className(s.getClassRoom() != null ? s.getClassRoom().getClassName() : null);
        }

        // Load teacher info if user is a teacher
        Optional<Teacher> teacher = teacherRepository.findByUserId(userId);
        if (teacher.isPresent()) {
            Teacher t = teacher.get();
            builder.teacherId(t.getId())
                   .dob(t.getDob())
                   .gender(t.getGender())
                   .address(t.getAddress())
                   .phone(t.getPhone())
                   .subject(t.getSubject());
        }

        return builder.build();
    }

    @Transactional
    public void changePassword(Long userId, ChangePasswordRequest request) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy người dùng!"));

        // Verify current password
        if (!passwordEncoder.matches(request.getCurrentPassword(), user.getPassword())) {
            throw new RuntimeException("Mật khẩu hiện tại không đúng!");
        }

        // Check if new password matches confirmation
        if (!request.getNewPassword().equals(request.getConfirmPassword())) {
            throw new RuntimeException("Mật khẩu mới và xác nhận mật khẩu không khớp!");
        }

        // Update password
        user.setPassword(passwordEncoder.encode(request.getNewPassword()));
        userRepository.save(user);
    }

    @Transactional
    public UserProfileResponse updateProfile(Long userId, UpdateProfileRequest request) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy người dùng!"));

        // Update user table
        if (request.getFullName() != null) {
            user.setFullName(request.getFullName());
        }
        if (request.getEmail() != null) {
            user.setEmail(request.getEmail());
        }
        userRepository.save(user);

        // Update student/teacher table if exists
        Optional<Student> student = studentRepository.findByUserId(userId);
        if (student.isPresent()) {
            Student s = student.get();
            if (request.getPhone() != null) s.setPhone(request.getPhone());
            if (request.getAddress() != null) s.setAddress(request.getAddress());
            studentRepository.save(s);
        }

        Optional<Teacher> teacher = teacherRepository.findByUserId(userId);
        if (teacher.isPresent()) {
            Teacher t = teacher.get();
            if (request.getPhone() != null) t.setPhone(request.getPhone());
            if (request.getAddress() != null) t.setAddress(request.getAddress());
            teacherRepository.save(t);
        }

        return getProfile(userId);
    }
}
