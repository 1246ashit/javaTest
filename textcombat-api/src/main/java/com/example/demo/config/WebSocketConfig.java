package com.example.demo.config;

import com.example.demo.security.JwtUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.server.ServerHttpRequest;
import org.springframework.http.server.ServerHttpResponse;
import org.springframework.http.server.ServletServerHttpRequest;
import org.springframework.web.socket.WebSocketHandler;
import org.springframework.web.socket.config.annotation.EnableWebSocketMessageBroker;
import org.springframework.web.socket.config.annotation.StompEndpointRegistry;
import org.springframework.web.socket.config.annotation.WebSocketMessageBrokerConfigurer;
import org.springframework.web.socket.server.HandshakeInterceptor;
import org.springframework.messaging.simp.config.MessageBrokerRegistry;

import java.security.Principal;
import java.util.Map;

/**
 * WebSocket / STOMP 設定。
 * - 端點：/ws（client 連線用）
 * - 訂閱前綴：/topic（廣播）
 * - JWT 驗證：透過 query string ?token=xxx 在 handshake 階段驗
 */
@Configuration
@EnableWebSocketMessageBroker
public class WebSocketConfig implements WebSocketMessageBrokerConfigurer {

    private static final Logger log = LoggerFactory.getLogger(WebSocketConfig.class);

    private final JwtUtil jwtUtil;

    public WebSocketConfig(JwtUtil jwtUtil) {
        this.jwtUtil = jwtUtil;
    }

    @Override
    public void configureMessageBroker(MessageBrokerRegistry registry) {
        registry.enableSimpleBroker("/topic");
        // client 送訊息給 server 的前綴（v1 用不到，但保留）
        registry.setApplicationDestinationPrefixes("/app");
    }

    @Override
    public void registerStompEndpoints(StompEndpointRegistry registry) {
        registry.addEndpoint("/ws")
                .setAllowedOriginPatterns("*")
                .addInterceptors(new HandshakeInterceptor() {
                    @Override
                    public boolean beforeHandshake(ServerHttpRequest request, ServerHttpResponse response,
                                                   WebSocketHandler wsHandler, Map<String, Object> attributes) {
                        String token = null;
                        if (request instanceof ServletServerHttpRequest http) {
                            token = http.getServletRequest().getParameter("token");
                        }
                        if (token == null || token.isBlank()) {
                            log.warn("WebSocket 拒絕連線: 缺 token");
                            return false;
                        }
                        try {
                            Long userId = jwtUtil.extractUserId(token);
                            String username = String.valueOf(jwtUtil.parse(token).get("username"));
                            attributes.put("userId", userId);
                            attributes.put("username", username);
                            attributes.put("__principal__", new StompPrincipal(userId, username));
                            return true;
                        } catch (Exception e) {
                            log.warn("WebSocket 拒絕連線: token 無效 ({})", e.getMessage());
                            return false;
                        }
                    }

                    @Override
                    public void afterHandshake(ServerHttpRequest req, ServerHttpResponse res,
                                               WebSocketHandler h, Exception ex) { }
                })
                .setHandshakeHandler(new org.springframework.web.socket.server.support.DefaultHandshakeHandler() {
                    @Override
                    protected Principal determineUser(ServerHttpRequest request, WebSocketHandler wsHandler,
                                                      Map<String, Object> attributes) {
                        Object p = attributes.get("__principal__");
                        return p instanceof Principal pp ? pp : null;
                    }
                });
    }

    /** WebSocket 連線後綁定的 user identity，messagingTemplate.convertAndSendToUser 用得到。 */
    public record StompPrincipal(Long userId, String username) implements Principal {
        @Override
        public String getName() { return String.valueOf(userId); }
    }
}
