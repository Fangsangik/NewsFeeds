package com.example.newsfeed.constants;


import com.example.newsfeed.auth.jwt.service.JwtProvider;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.server.ServerHttpRequest;
import org.springframework.http.server.ServerHttpResponse;
import org.springframework.web.socket.WebSocketHandler;
import org.springframework.web.socket.server.HandshakeInterceptor;

import java.util.List;
import java.util.Map;

/**
 * WebSocket 연결 시 JWT 토큰을 검증하는 인터셉터
 */
@Slf4j
public class JwtHandShakeInterceptor implements HandshakeInterceptor {

    private final JwtProvider jwtProvider;

    public JwtHandShakeInterceptor(JwtProvider jwtProvider) {
        this.jwtProvider = jwtProvider;
    }

    @Override
    public boolean beforeHandshake(ServerHttpRequest request, ServerHttpResponse response, WebSocketHandler wsHandler, Map<String, Object> attributes) throws Exception {
        try {
            log.info("WebSocket Handshake 시작");
            List<String> header = request.getHeaders().get("Authorization");
            if (header == null || header.isEmpty()) {
                log.error("Websocket Handshake Error: No Token");
                return false;
            }

            String jwtToken = header.get(0).replace("Bearer ", "");
            log.info("WebSocket Token: {}", jwtToken);

            if (jwtToken != null && jwtProvider.validateToken(jwtToken)) {
                Long memberId = jwtProvider.getUserIdFromToken(jwtToken);
                attributes.put("memberId", memberId);
                log.info("WebSocket Handshake Success, 사용자 ID: {}", memberId);
                return true;
            }

            log.warn("Websocket Handshake Error: Invalid Token");
            return false;
        } catch (Exception e) {
            log.error("Websocket Handshake Error", e);
            return false;
        }
    }


    @Override
    public void afterHandshake(ServerHttpRequest request, ServerHttpResponse response, WebSocketHandler wsHandler, Exception exception) {
        if (exception != null) {
            log.error("Websocket Handshake Error", exception);
        }

        if (exception == null) {
            log.info("Websocket Handshake Success");
        }
    }
}

