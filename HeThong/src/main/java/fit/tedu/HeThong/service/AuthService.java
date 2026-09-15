package fit.tedu.HeThong.service;

import fit.tedu.HeThong.dto.request.LoginRequest;
import fit.tedu.HeThong.dto.request.RegisterRequest;
import fit.tedu.HeThong.dto.response.AuthResponse;
import fit.tedu.HeThong.entity.Role;
import fit.tedu.HeThong.entity.User;
import fit.tedu.HeThong.repository.RoleRepository;
import fit.tedu.HeThong.repository.UserRepository;
import fit.tedu.HeThong.security.JwtUtils;
import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.*;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Set;

@Service
@RequiredArgsConstructor
public class AuthService {

        private static final Set<String> ALLOWED_ROLES = Set.of(
                        "ADMIN", "TEACHER", "STUDENT", "ACCOUNTANT", "CONTENT_MANAGER",
                        "ACADEMIC_AFFAIRS", "MANAGER");

    private final UserRepository userRepository;
    private final RoleRepository roleRepository;
    private final PasswordEncoder passwordEncoder;
    private final AuthenticationManager authenticationManager;
    private final JwtUtils jwtUtils;

    public AuthResponse login(LoginRequest request) {
        Authentication authentication = authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(request.getUsername(), request.getPassword()));

        UserDetails userDetails = (UserDetails) authentication.getPrincipal();
        String token = jwtUtils.generateToken(userDetails);

        User user = userRepository.findByUsername(request.getUsername()).orElseThrow();
        return AuthResponse.builder()
                .token(token)
                .type("Bearer")
                .id(user.getId())
                .username(user.getUsername())
                .email(user.getEmail())
                .fullName(user.getFullName())
                .role(user.getRole().getName())
                .build();
    }

    @Transactional
    public AuthResponse register(RegisterRequest request) {
        if (userRepository.existsByUsername(request.getUsername())) {
            throw new RuntimeException("Tên đăng nhập đã tồn tại!");
        }
        if (userRepository.existsByEmail(request.getEmail())) {
            throw new RuntimeException("Email đã được sử dụng!");
        }

        String roleName = (request.getRole() != null) ? request.getRole().trim().toUpperCase() : "STUDENT";
        if (!ALLOWED_ROLES.contains(roleName)) {
            throw new RuntimeException("Vai trò không hợp lệ");
        }
        Role role = roleRepository.findByName(roleName)
                .orElseGet(() -> roleRepository.save(new Role(null, roleName)));

        User user = User.builder()
                .username(request.getUsername())
                .password(passwordEncoder.encode(request.getPassword()))
                .email(request.getEmail())
                .fullName(request.getFullName())
                .role(role)
                .enabled(true)
                .build();

        userRepository.save(user);

        // Auto-login after register
        UserDetails userDetails = org.springframework.security.core.userdetails.User
                .withUsername(user.getUsername())
                .password(user.getPassword())
                .authorities(role.getName())
                .build();
        String token = jwtUtils.generateToken(userDetails);

        return AuthResponse.builder()
                .token(token)
                .type("Bearer")
                .id(user.getId())
                .username(user.getUsername())
                .email(user.getEmail())
                .fullName(user.getFullName())
                .role(role.getName())
                .build();
    }
}
