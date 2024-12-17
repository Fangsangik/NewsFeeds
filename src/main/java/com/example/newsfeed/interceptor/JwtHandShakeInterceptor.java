package com.example.newsfeed.interceptor;

import com.example.newsfeed.auth.jwt.JwtProvider;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.server.ServerHttpRequest;
import org.springframework.http.server.ServerHttpResponse;
import org.springframework.web.socket.WebSocketHandler;
import org.springframework.web.socket.server.HandshakeInterceptor;

import java.util.List;
import java.util.Map;

@Slf4j
public class JwtHandShakeInterceptor implements HandshakeInterceptor {

    private final JwtProvider jwtProvider;

    public JwtHandShakeInterceptor(JwtProvider jwtProvider) {
        this.jwtProvider = jwtProvider;
    }

    @Override
    public boolean beforeHandshake(ServerHttpRequest request, ServerHttpResponse response, WebSocketHandler wsHandler, Map<String, Object> attributes) throws Exception {

        try {
            //Http 요청 Header는 같은 키를 여러번 갖을 수 있음
            //Http 스팩당 하나의 헤더 키에 여러값 갖을 수 있기 때문에 List로 반환
            List<String> header = request.getHeaders().get("Authorization");
            if (header == null || header.isEmpty()) {
                log.error("Websocket Handshake Error: No Token");
                return false;
            }

            String jwtToken = header.get(0).replace("Bearer ", "");
            if (jwtToken != null && jwtProvider.validateToken(jwtToken)) {
                Long memberId = jwtProvider.getUserIdFromToken(jwtToken);
                attributes.put("memberId", memberId);
                log.info("Websocket Handshake Success");
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
