package com.example.demo.controller;

import com.example.demo.dto.LoginRequest;
import com.example.demo.dto.LoginResponse;
import com.example.demo.dto.RegisterRequest;
import com.example.demo.dto.UserResponse;
import com.example.demo.entity.UsersEntity;
import com.example.demo.repository.UserRepository;
import com.example.demo.security.CurrentUserHolder;
import com.example.demo.security.JwtUtil;
import com.example.demo.security.Public;
import com.example.demo.security.RequirePermission;
import com.example.demo.service.AccountService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api")
public class AccountController {

    private final AccountService accountService;
    private final JwtUtil jwtUtil;
    private final UserRepository userRepository;

    public AccountController(AccountService accountService, JwtUtil jwtUtil, UserRepository userRepository) {
        this.accountService = accountService;
        this.jwtUtil = jwtUtil;
        this.userRepository = userRepository;
    }

    // ===== 公開 API（@Public：不用登入就能打）=====

    @Public
    @PostMapping("/auth/register")
    public ResponseEntity<?> register(@RequestBody RegisterRequest req) {
        try {
            UsersEntity user = accountService.register(req);
            return ResponseEntity.ok(UserResponse.from(user));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        } catch (IllegalStateException e) {
            return ResponseEntity.status(409).body(Map.of("error", e.getMessage()));
        }
    }

    @Public
    @PostMapping("/auth/login")
    public ResponseEntity<?> login(@RequestBody LoginRequest req) {
        try {
            UsersEntity user = accountService.login(req);
            String token = jwtUtil.generate(user.getId(), user.getUsername());
            return ResponseEntity.ok(new LoginResponse(token, UserResponse.from(user)));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        } catch (IllegalStateException e) {
            return ResponseEntity.status(401).body(Map.of("error", e.getMessage()));
        }
    }

    // ===== 受保護 API（預設需登入）=====

    // 看自己的資料（登入就能打）
    @GetMapping("/me")
    public ResponseEntity<?> me() {
        return ResponseEntity.ok(UserResponse.from(CurrentUserHolder.get()));
    }

    // 管理員：看全部使用者
    @GetMapping("/users")
    @RequirePermission("USER_READ_ALL")
    public ResponseEntity<?> listAllUsers() {
        List<UserResponse> users = userRepository.findAll().stream()
                .map(UserResponse::from)
                .toList();
        return ResponseEntity.ok(users);
    }
}
