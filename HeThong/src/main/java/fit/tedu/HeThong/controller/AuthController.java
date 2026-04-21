package fit.tedu.HeThong.controller;

import fit.tedu.HeThong.dto.request.LoginRequest;
import fit.tedu.HeThong.dto.request.RegisterRequest;
import fit.tedu.HeThong.dto.response.ApiResponse;
import fit.tedu.HeThong.dto.response.AuthResponse;
import fit.tedu.HeThong.service.AuthService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;

    @PostMapping("/login")
    public ResponseEntity<ApiResponse<AuthResponse>> login(@Valid @RequestBody LoginRequest request) {
        try {
            AuthResponse response = authService.login(request);
            return ResponseEntity.ok(ApiResponse.ok("Đăng nhập thành công", response));
        } catch (Exception e) {
            return ResponseEntity.status(401).body(ApiResponse.error("Sai tên đăng nhập hoặc mật khẩu!"));
        }
    }

    @PostMapping("/register")
    public ResponseEntity<ApiResponse<AuthResponse>> register(@Valid @RequestBody RegisterRequest request) {
        try {
            AuthResponse response = authService.register(request);
            return ResponseEntity.ok(ApiResponse.ok("Đăng ký thành công", response));
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(ApiResponse.error(e.getMessage()));
        }
    }
}
