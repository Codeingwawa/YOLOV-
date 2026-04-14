package com.parking.controller;

import com.parking.service.AuthService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
@Tag(name = "认证管理", description = "用户认证接口")
public class AuthController {
    
    private final AuthService authService;
    
    @PostMapping("/login")
    @Operation(summary = "用户登录", description = "用户登录获取token")
    public ResponseEntity<?> login(@RequestBody LoginRequest request) {
        AuthService.AuthResult result = authService.login(request.getUsername(), request.getPassword());
        
        if (result.isSuccess()) {
            Map<String, Object> response = new HashMap<>();
            response.put("token", result.getToken());
            response.put("username", result.getUser().getUsername());
            response.put("realName", result.getUser().getRealName());
            response.put("role", result.getUser().getRole());
            return ResponseEntity.ok(response);
        } else {
            Map<String, String> error = new HashMap<>();
            error.put("error", result.getMessage());
            return ResponseEntity.status(401).body(error);
        }
    }
    
    @PostMapping("/register")
    @Operation(summary = "用户注册", description = "注册新用户")
    public ResponseEntity<?> register(@RequestBody RegisterRequest request) {
        AuthService.AuthResult result = authService.register(
            request.getUsername(), 
            request.getPassword(), 
            request.getRealName()
        );
        
        if (result.isSuccess()) {
            Map<String, Object> response = new HashMap<>();
            response.put("token", result.getToken());
            response.put("username", result.getUser().getUsername());
            response.put("realName", result.getUser().getRealName());
            response.put("role", result.getUser().getRole());
            return ResponseEntity.ok(response);
        } else {
            Map<String, String> error = new HashMap<>();
            error.put("error", result.getMessage());
            return ResponseEntity.status(400).body(error);
        }
    }
    
    @GetMapping("/check")
    @Operation(summary = "检查登录状态", description = "检查用户登录状态")
    public ResponseEntity<?> checkAuth(@RequestHeader(value = "Authorization", required = false) String authHeader) {
        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            Map<String, Boolean> response = new HashMap<>();
            response.put("authenticated", false);
            return ResponseEntity.ok(response);
        }
        
        Map<String, Boolean> response = new HashMap<>();
        response.put("authenticated", true);
        return ResponseEntity.ok(response);
    }
    
    @PutMapping("/profile")
    @Operation(summary = "修改个人设置", description = "修改真实姓名和密码")
    public ResponseEntity<?> updateProfile(@RequestHeader("Authorization") String authHeader,
                                            @RequestBody ProfileRequest request) {
        String token = authHeader.replace("Bearer ", "");
        String username = authService.extractUsername(token);
        
        if (username == null) {
            Map<String, String> error = new HashMap<>();
            error.put("error", "无效的登录状态");
            return ResponseEntity.status(401).body(error);
        }
        
        AuthService.ProfileResult result = authService.updateProfile(
            username, request.getRealName(), request.getOldPassword(), request.getNewPassword()
        );
        
        if (result.isSuccess()) {
            Map<String, String> response = new HashMap<>();
            response.put("message", result.getMessage());
            return ResponseEntity.ok(response);
        } else {
            Map<String, String> error = new HashMap<>();
            error.put("error", result.getMessage());
            return ResponseEntity.status(400).body(error);
        }
    }
    
    @Data
    public static class LoginRequest {
        private String username;
        private String password;
    }
    
    @Data
    public static class RegisterRequest {
        private String username;
        private String password;
        private String realName;
    }
    
    @Data
    public static class ProfileRequest {
        private String realName;
        private String oldPassword;
        private String newPassword;
    }
}
