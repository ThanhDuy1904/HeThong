package fit.tedu.HeThong.controller;

import fit.tedu.HeThong.dto.request.ChangePasswordRequest;
import fit.tedu.HeThong.dto.request.AdminUserRequest;
import fit.tedu.HeThong.dto.response.AdminUserResponse;
import fit.tedu.HeThong.dto.request.UpdateProfileRequest;
import fit.tedu.HeThong.dto.response.ApiResponse;
import fit.tedu.HeThong.dto.response.UserProfileResponse;
import fit.tedu.HeThong.entity.User;
import fit.tedu.HeThong.service.UserService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;
import java.util.List;

@RestController
@RequestMapping("/api/user")
@RequiredArgsConstructor
public class UserController {

    private final UserService userService;

    @GetMapping("/accounts")
    public ResponseEntity<ApiResponse<List<AdminUserResponse>>> getAccounts() {
        return ResponseEntity.ok(ApiResponse.ok(userService.getAllAccounts()));
    }

    @PostMapping("/accounts")
    public ResponseEntity<ApiResponse<AdminUserResponse>> createAccount(
            @Valid @RequestBody AdminUserRequest request) {
        try {
            return ResponseEntity.ok(ApiResponse.ok("Tạo tài khoản thành công", userService.createAccount(request)));
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(ApiResponse.error(e.getMessage()));
        }
    }

    @PutMapping("/accounts/{id}")
    public ResponseEntity<ApiResponse<AdminUserResponse>> updateAccount(
            @PathVariable Long id, @Valid @RequestBody AdminUserRequest request) {
        try {
            return ResponseEntity.ok(ApiResponse.ok("Cập nhật tài khoản thành công", userService.updateAccount(id, request)));
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(ApiResponse.error(e.getMessage()));
        }
    }

    @DeleteMapping("/accounts/{id}")
    public ResponseEntity<ApiResponse<Void>> deleteAccount(@PathVariable Long id) {
        try {
            userService.deleteAccount(id);
            return ResponseEntity.ok(ApiResponse.ok("Xóa tài khoản thành công", null));
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(ApiResponse.error(e.getMessage()));
        }
    }

    @GetMapping("/profile")
    public ResponseEntity<ApiResponse<UserProfileResponse>> getProfile() {
        try {
            Authentication auth = SecurityContextHolder.getContext().getAuthentication();
            if (auth == null || auth.getPrincipal() == null) {
                return ResponseEntity.status(401).body(ApiResponse.error("Chưa đăng nhập"));
            }
            
            User user = (User) auth.getPrincipal();
            UserProfileResponse profile = userService.getProfile(user.getId());
            return ResponseEntity.ok(ApiResponse.ok(profile));
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(ApiResponse.error(e.getMessage()));
        }
    }

    @PutMapping("/change-password")
    public ResponseEntity<ApiResponse<Void>> changePassword(
            @Valid @RequestBody ChangePasswordRequest request) {
        try {
            Authentication auth = SecurityContextHolder.getContext().getAuthentication();
            if (auth == null || auth.getPrincipal() == null) {
                return ResponseEntity.status(401).body(ApiResponse.error("Chưa đăng nhập"));
            }
            
            User user = (User) auth.getPrincipal();
            userService.changePassword(user.getId(), request);
            return ResponseEntity.ok(ApiResponse.ok("Đổi mật khẩu thành công", null));
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(ApiResponse.error(e.getMessage()));
        }
    }

    @PutMapping("/profile")
    public ResponseEntity<ApiResponse<UserProfileResponse>> updateProfile(
            @Valid @RequestBody UpdateProfileRequest request) {
        try {
            Authentication auth = SecurityContextHolder.getContext().getAuthentication();
            if (auth == null || auth.getPrincipal() == null) {
                return ResponseEntity.status(401).body(ApiResponse.error("Chưa đăng nhập"));
            }
            
            User user = (User) auth.getPrincipal();
            UserProfileResponse profile = userService.updateProfile(user.getId(), request);
            return ResponseEntity.ok(ApiResponse.ok("Cập nhật thông tin thành công", profile));
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(ApiResponse.error(e.getMessage()));
        }
    }
}
