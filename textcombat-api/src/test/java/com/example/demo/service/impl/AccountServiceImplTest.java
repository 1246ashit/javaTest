package com.example.demo.service.impl;

import com.example.demo.dto.LoginRequest;
import com.example.demo.dto.RegisterRequest;
import com.example.demo.entity.Role;
import com.example.demo.entity.UsersEntity;
import com.example.demo.repository.RoleRepository;
import com.example.demo.repository.UserRepository;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

import static org.mockito.Mockito.when;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.never;
import static org.mockito.ArgumentMatchers.any;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.time.OffsetDateTime;
import java.util.Optional;

@ExtendWith(MockitoExtension.class)
public class AccountServiceImplTest {
    @Mock
    private UserRepository userRepository;
    @Mock
    private RoleRepository roleRepository;
    @Mock
    private PasswordEncoder passwordEncoder;

    @InjectMocks
    private AccountServiceImpl accountServiceImpl;

    // register
    @Test
    @DisplayName("case1: register happy path 註冊成功")
    void register_case1() {
        // given
        RegisterRequest req = new RegisterRequest();
        req.setUsername("gash7514");
        req.setPassword("gash8024");
        req.setDisplayName("神風o烈士");

        Role defaultRole = new Role();

        when(roleRepository.findByCode("PLAYER")).thenReturn(Optional.of(defaultRole));
        when(passwordEncoder.encode("gash8024")).thenReturn("HASH");
        when(userRepository.save(any(UsersEntity.class)))
                .thenAnswer(inv -> inv.getArgument(0));

        // act
        accountServiceImpl.register(req);

        // assert
        verify(passwordEncoder).encode(req.getPassword());// 密碼加密有執行
        verify(userRepository).existsByUsername("gash7514");

        ArgumentCaptor<UsersEntity> cap = ArgumentCaptor.forClass(UsersEntity.class);
        verify(userRepository).save(cap.capture());

        UsersEntity saved = cap.getValue();
        assertThat(saved.getUsername()).isEqualTo("gash7514");
        assertThat(saved.getPasswordHash()).isEqualTo("HASH"); // ← 守密碼不變式
        assertThat(saved.getPasswordHash()).isNotEqualTo("gash8024"); // 不是明碼
        assertThat(saved.getGold()).isEqualTo(100L);
        assertThat(saved.getIsActive()).isTrue();
        assertThat(saved.getRoles()).hasSize(1).contains(defaultRole);
    }
    //

    // login
    @Test
    @DisplayName("case1: login happy path 登入成功")
    void login_case1() {
        // given
        LoginRequest req = new LoginRequest();
        req.setUsername("gash7514");
        req.setPassword("gash8024");

        UsersEntity user=new UsersEntity();
        user.setIsActive(true);
        user.setDisplayName("神風o烈士");
        user.setPasswordHash("HASH");
        
        when(userRepository.findByUsername(req.getUsername())).thenReturn(Optional.of(user));
        when(passwordEncoder.matches(req.getPassword(), user.getPasswordHash())).thenReturn(true);
        when(userRepository.save(any(UsersEntity.class)))
                .thenAnswer(inv -> inv.getArgument(0));
        // act
        OffsetDateTime before = OffsetDateTime.now();
        UsersEntity result = accountServiceImpl.login(req);
        OffsetDateTime after = OffsetDateTime.now();

        // assert
        verify(passwordEncoder).matches("gash8024", "HASH");
        assertThat(result).isSameAs(user);
        
        ArgumentCaptor<UsersEntity> cap = ArgumentCaptor.forClass(UsersEntity.class);
        verify(userRepository).save(cap.capture());

        UsersEntity saved = cap.getValue();
        assertThat(saved).isSameAs(user);
        assertThat(saved.getDisplayName()).isEqualTo("神風o烈士");
        assertThat(saved.getLastLoginAt()).isBetween(before, after);

    }
    //

    // ===== register: 錯誤 case =====

    @Test
    @DisplayName("register_case2: username < 3 字元應拋 IllegalArgumentException、不該 save")
    void register_case2_usernameTooShort_throws() {
        RegisterRequest req = new RegisterRequest();
        req.setUsername("ab");
        req.setPassword("abcdef");

        assertThatThrownBy(() -> accountServiceImpl.register(req))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("username 需 3-32 字元");

        verify(userRepository, never()).existsByUsername(any());
        verify(userRepository, never()).save(any());
        verify(passwordEncoder, never()).encode(any());
    }

    @Test
    @DisplayName("register_case3: username > 32 字元應拋 IllegalArgumentException")
    void register_case3_usernameTooLong_throws() {
        RegisterRequest req = new RegisterRequest();
        req.setUsername("a".repeat(33));
        req.setPassword("abcdef");

        assertThatThrownBy(() -> accountServiceImpl.register(req))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("username 需 3-32 字元");

        verify(userRepository, never()).save(any());
    }

    @Test
    @DisplayName("register_case4: username = null 應拋 IllegalArgumentException")
    void register_case4_usernameNull_throws() {
        RegisterRequest req = new RegisterRequest();
        req.setUsername(null);
        req.setPassword("abcdef");

        assertThatThrownBy(() -> accountServiceImpl.register(req))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("username 需 3-32 字元");

        verify(userRepository, never()).save(any());
    }

    @Test
    @DisplayName("register_case5: password < 6 字元應拋 IllegalArgumentException")
    void register_case5_passwordTooShort_throws() {
        RegisterRequest req = new RegisterRequest();
        req.setUsername("alice");
        req.setPassword("12345");

        assertThatThrownBy(() -> accountServiceImpl.register(req))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("password 至少 6 字元");

        verify(userRepository, never()).existsByUsername(any());
        verify(passwordEncoder, never()).encode(any());
        verify(userRepository, never()).save(any());
    }

    @Test
    @DisplayName("register_case6: password = null 應拋 IllegalArgumentException")
    void register_case6_passwordNull_throws() {
        RegisterRequest req = new RegisterRequest();
        req.setUsername("alice");
        req.setPassword(null);

        assertThatThrownBy(() -> accountServiceImpl.register(req))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("password 至少 6 字元");
    }

    @Test
    @DisplayName("register_case7: 帳號重複應拋 IllegalStateException、密碼不該被加密")
    void register_case7_duplicateUsername_throws() {
        RegisterRequest req = new RegisterRequest();
        req.setUsername("alice");
        req.setPassword("abcdef");

        when(userRepository.existsByUsername("alice")).thenReturn(true);

        assertThatThrownBy(() -> accountServiceImpl.register(req))
                .isInstanceOf(IllegalStateException.class)
                .hasMessage("帳號已被使用");

        // 防禦：encode 是 BCrypt 很慢，重複帳號就不該白做
        verify(passwordEncoder, never()).encode(any());
        verify(userRepository, never()).save(any());
    }

    @Test
    @DisplayName("register_case8: 預設 PLAYER 角色不存在應拋 IllegalStateException")
    void register_case8_defaultRoleMissing_throws() {
        RegisterRequest req = new RegisterRequest();
        req.setUsername("alice");
        req.setPassword("abcdef");

        when(userRepository.existsByUsername("alice")).thenReturn(false);
        when(roleRepository.findByCode("PLAYER")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> accountServiceImpl.register(req))
                .isInstanceOf(IllegalStateException.class)
                .hasMessage("預設角色 PLAYER 不存在");

        verify(userRepository, never()).save(any());
    }

    // ===== login: 錯誤 case =====

    @Test
    @DisplayName("login_case2: username = null 應拋 IllegalArgumentException")
    void login_case2_usernameNull_throws() {
        LoginRequest req = new LoginRequest();
        req.setUsername(null);
        req.setPassword("abcdef");

        assertThatThrownBy(() -> accountServiceImpl.login(req))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("username 與 password 為必填");

        verify(userRepository, never()).findByUsername(any());
        verify(userRepository, never()).save(any());
    }

    @Test
    @DisplayName("login_case3: password = null 應拋 IllegalArgumentException")
    void login_case3_passwordNull_throws() {
        LoginRequest req = new LoginRequest();
        req.setUsername("alice");
        req.setPassword(null);

        assertThatThrownBy(() -> accountServiceImpl.login(req))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("username 與 password 為必填");

        verify(userRepository, never()).findByUsername(any());
    }

    @Test
    @DisplayName("login_case4: 帳號不存在應拋『帳號或密碼錯誤』（不揭露細節）")
    void login_case4_userNotFound_throws() {
        LoginRequest req = new LoginRequest();
        req.setUsername("ghost");
        req.setPassword("abcdef");

        when(userRepository.findByUsername("ghost")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> accountServiceImpl.login(req))
                .isInstanceOf(IllegalStateException.class)
                .hasMessage("帳號或密碼錯誤");          // ← 反 user enumeration

        verify(passwordEncoder, never()).matches(any(), any());
        verify(userRepository, never()).save(any());
    }

    @Test
    @DisplayName("login_case5: 密碼錯誤應拋『帳號或密碼錯誤』、不該更新 lastLoginAt / 不該 save")
    void login_case5_wrongPassword_throws() {
        LoginRequest req = new LoginRequest();
        req.setUsername("alice");
        req.setPassword("wrong");

        UsersEntity user = new UsersEntity();
        user.setPasswordHash("HASH");
        user.setIsActive(true);

        when(userRepository.findByUsername("alice")).thenReturn(Optional.of(user));
        when(passwordEncoder.matches("wrong", "HASH")).thenReturn(false);

        assertThatThrownBy(() -> accountServiceImpl.login(req))
                .isInstanceOf(IllegalStateException.class)
                .hasMessage("帳號或密碼錯誤");          // 跟 case4 一樣，刻意

        assertThat(user.getLastLoginAt()).isNull();
        verify(userRepository, never()).save(any());
    }

    @Test
    @DisplayName("login_case6: 帳號停用（isActive=false）應拋『帳號已停用』")
    void login_case6_inactiveUser_throws() {
        LoginRequest req = new LoginRequest();
        req.setUsername("alice");
        req.setPassword("abcdef");

        UsersEntity user = new UsersEntity();
        user.setPasswordHash("HASH");
        user.setIsActive(false);

        when(userRepository.findByUsername("alice")).thenReturn(Optional.of(user));
        when(passwordEncoder.matches("abcdef", "HASH")).thenReturn(true);

        assertThatThrownBy(() -> accountServiceImpl.login(req))
                .isInstanceOf(IllegalStateException.class)
                .hasMessage("帳號已停用");

        assertThat(user.getLastLoginAt()).isNull();
        verify(userRepository, never()).save(any());
    }
}
