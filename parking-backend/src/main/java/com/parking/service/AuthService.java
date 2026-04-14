package com.parking.service;

import com.parking.entity.User;
import com.parking.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Slf4j
public class AuthService {
    
    private final UserRepository userRepository;
    private final JwtService jwtService;
    private final PasswordEncoder passwordEncoder;
    
    @Transactional
    public AuthResult login(String username, String password) {
        User user = userRepository.findByUsername(username)
            .orElse(null);
        
        if (user == null) {
            return AuthResult.failure("用户不存在");
        }
        
        if (!passwordEncoder.matches(password, user.getPassword())) {
            return AuthResult.failure("密码错误");
        }
        
        String token = jwtService.generateToken(user.getUsername(), user.getRole());
        
        return AuthResult.success(token, user);
    }
    
    @Transactional
    public AuthResult register(String username, String password, String realName) {
        if (userRepository.existsByUsername(username)) {
            return AuthResult.failure("用户名已存在");
        }
        
        User user = new User();
        user.setUsername(username);
        user.setPassword(passwordEncoder.encode(password));
        user.setRealName(realName);
        user.setRole("USER");
        
        userRepository.save(user);
        
        String token = jwtService.generateToken(user.getUsername(), user.getRole());
        
        return AuthResult.success(token, user);
    }
    
    public String extractUsername(String token) {
        try {
            return jwtService.extractUsername(token);
        } catch (Exception e) {
            return null;
        }
    }
    
    @Transactional
    public ProfileResult updateProfile(String username, String realName, String oldPassword, String newPassword) {
        User user = userRepository.findByUsername(username).orElse(null);
        
        if (user == null) {
            return ProfileResult.failure("用户不存在");
        }
        
        if (realName != null && !realName.isEmpty()) {
            user.setRealName(realName);
        }
        
        if (newPassword != null && !newPassword.isEmpty()) {
            if (oldPassword == null || oldPassword.isEmpty()) {
                return ProfileResult.failure("修改密码需要输入当前密码");
            }
            if (!passwordEncoder.matches(oldPassword, user.getPassword())) {
                return ProfileResult.failure("当前密码错误");
            }
            user.setPassword(passwordEncoder.encode(newPassword));
        }
        
        userRepository.save(user);
        return ProfileResult.success("保存成功");
    }
    
    public static class AuthResult {
        private boolean success;
        private String message;
        private String token;
        private User user;
        
        public static AuthResult success(String token, User user) {
            AuthResult result = new AuthResult();
            result.success = true;
            result.token = token;
            result.user = user;
            result.message = "登录成功";
            return result;
        }
        
        public static AuthResult failure(String message) {
            AuthResult result = new AuthResult();
            result.success = false;
            result.message = message;
            return result;
        }
        
        public boolean isSuccess() { return success; }
        public String getMessage() { return message; }
        public String getToken() { return token; }
        public User getUser() { return user; }
    }
    
    public static class ProfileResult {
        private boolean success;
        private String message;
        
        public static ProfileResult success(String message) {
            ProfileResult result = new ProfileResult();
            result.success = true;
            result.message = message;
            return result;
        }
        
        public static ProfileResult failure(String message) {
            ProfileResult result = new ProfileResult();
            result.success = false;
            result.message = message;
            return result;
        }
        
        public boolean isSuccess() { return success; }
        public String getMessage() { return message; }
    }
}
