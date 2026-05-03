package com.example.demo.security;

import com.example.demo.entity.UsersEntity;
import com.example.demo.repository.UserRepository;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.method.HandlerMethod;
import org.springframework.web.servlet.HandlerInterceptor;

import java.nio.charset.StandardCharsets;
import java.util.Optional;

@Component
public class JwtAuthInterceptor implements HandlerInterceptor {

    private static final Logger log = LoggerFactory.getLogger(JwtAuthInterceptor.class);

    private final JwtUtil jwtUtil;
    private final UserRepository userRepository;

    public JwtAuthInterceptor(JwtUtil jwtUtil, UserRepository userRepository) {
        this.jwtUtil = jwtUtil;
        this.userRepository = userRepository;
    }

    @Override
    public boolean preHandle(HttpServletRequest req, HttpServletResponse res, Object handler) throws Exception {

        // 只攔 Controller method，靜態資源等直接放行
        if (!(handler instanceof HandlerMethod hm)) {
            return true;
        }

        // 嘗試解析 token（不論 @Public 與否都先解，方便 @Public endpoint 也能讀 currentUser）
        String auth = req.getHeader("Authorization");
        if (auth != null && auth.startsWith("Bearer ")) {
            String token = auth.substring(7);
            try {
                Long userId = jwtUtil.extractUserId(token);
                Optional<UsersEntity> userOpt = userRepository.findById(userId);
                userOpt.ifPresent(CurrentUserHolder::set);
            } catch (Exception e) {
                log.warn("Token 解析失敗: path={}, reason={}", req.getRequestURI(), e.getMessage());
            }
        }

        // @Public → 完全開放，不看登入也不看權限
        if (isPublic(hm)) {
            return true;
        }

        // 預設規則：必須登入
        UsersEntity user = CurrentUserHolder.get();
        if (user == null) {
            log.warn("未登入: path={}", req.getRequestURI());
            return writeError(res, HttpStatus.UNAUTHORIZED, "未登入或 token 無效");
        }

        // 額外 @RequirePermission 檢查（有標才擋）
        RequirePermission require = hm.getMethodAnnotation(RequirePermission.class);
        if (require == null) {
            require = hm.getBeanType().getAnnotation(RequirePermission.class);
        }
        if (require != null && !user.hasPermission(require.value())) {
            log.warn("權限不足: userId={}, username={}, required={}, path={}",
                    user.getId(), user.getUsername(), require.value(), req.getRequestURI());
            return writeError(res, HttpStatus.FORBIDDEN, "沒有權限: " + require.value());
        }

        return true;
    }

    @Override
    public void afterCompletion(HttpServletRequest req, HttpServletResponse res, Object handler, Exception ex) {
        CurrentUserHolder.clear();   // 避免 ThreadLocal 記憶體洩漏
    }

    private boolean isPublic(HandlerMethod hm) {
        if (hm.getMethodAnnotation(Public.class) != null) return true;
        return hm.getBeanType().getAnnotation(Public.class) != null;
    }

    private boolean writeError(HttpServletResponse res, HttpStatus status, String message) throws Exception {
        res.setStatus(status.value());
        res.setContentType(MediaType.APPLICATION_JSON_VALUE);
        res.setCharacterEncoding(StandardCharsets.UTF_8.name());
        res.getWriter().write("{\"error\":\"" + message + "\"}");
        return false;
    }
}
