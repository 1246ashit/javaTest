package com.example.demo.Services;

import com.example.demo.DTO.LoginRequest;
import com.example.demo.DTO.RegisterRequest;
import com.example.demo.Entities.Role;
import com.example.demo.Entities.UsersEntity;
import com.example.demo.Repository.RoleRepository;
import com.example.demo.Repository.UserRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.OffsetDateTime;
import java.util.HashSet;
import java.util.Set;

@Service
public class AccountService {

    private static final Logger log = LoggerFactory.getLogger(AccountService.class);
    private static final String DEFAULT_ROLE_CODE = "PLAYER";

    private final UserRepository userRepository;
    private final RoleRepository roleRepository;
    private final PasswordEncoder passwordEncoder = new BCryptPasswordEncoder();

    public AccountService(UserRepository userRepository, RoleRepository roleRepository) {
        this.userRepository = userRepository;
        this.roleRepository = roleRepository;
    }

    @Transactional
    public UsersEntity register(RegisterRequest req) {
        if (req.getUsername() == null || req.getUsername().length() < 3 || req.getUsername().length() > 32) {
            throw new IllegalArgumentException("username 需 3-32 字元");
        }
        if (req.getPassword() == null || req.getPassword().length() < 6) {
            throw new IllegalArgumentException("password 至少 6 字元");
        }

        if (userRepository.existsByUsername(req.getUsername())) {
            log.warn("註冊失敗 (帳號重複): username={}", req.getUsername());
            throw new IllegalStateException("帳號已被使用");
        }

        Role defaultRole = roleRepository.findByCode(DEFAULT_ROLE_CODE)
                .orElseThrow(() -> new IllegalStateException("預設角色 " + DEFAULT_ROLE_CODE + " 不存在"));

        UsersEntity user = new UsersEntity();
        user.setUsername(req.getUsername());
        user.setPasswordHash(passwordEncoder.encode(req.getPassword()));
        user.setDisplayName(req.getDisplayName());
        user.setIsActive(true);
        user.setGold(100L);   // 新玩家起始金幣

        Set<Role> roles = new HashSet<>();
        roles.add(defaultRole);
        user.setRoles(roles);

        UsersEntity saved = userRepository.save(user);
        log.info("註冊成功: username={}, id={}, role={}", saved.getUsername(), saved.getId(), DEFAULT_ROLE_CODE);
        return saved;
    }

    @Transactional
    public UsersEntity login(LoginRequest req) {
        if (req.getUsername() == null || req.getPassword() == null) {
            throw new IllegalArgumentException("username 與 password 為必填");
        }

        UsersEntity user = userRepository.findByUsername(req.getUsername()).orElse(null);
        if (user == null) {
            log.warn("登入失敗 (帳號不存在): username={}", req.getUsername());
            throw new IllegalStateException("帳號或密碼錯誤");
        }

        if (!passwordEncoder.matches(req.getPassword(), user.getPasswordHash())) {
            log.warn("登入失敗 (密碼錯誤): username={}, id={}", user.getUsername(), user.getId());
            throw new IllegalStateException("帳號或密碼錯誤");
        }

        if (Boolean.FALSE.equals(user.getIsActive())) {
            log.warn("登入失敗 (帳號停用): username={}, id={}", user.getUsername(), user.getId());
            throw new IllegalStateException("帳號已停用");
        }

        user.setLastLoginAt(OffsetDateTime.now());
        UsersEntity saved = userRepository.save(user);
        log.info("登入成功: username={}, id={}, roles={}",
                saved.getUsername(), saved.getId(),
                saved.getRoles().stream().map(Role::getCode).toList());
        return saved;
    }
}
