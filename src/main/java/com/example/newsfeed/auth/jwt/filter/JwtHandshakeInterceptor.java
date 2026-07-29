package com.example.newsfeed.auth.jwt.filter;

import com.example.newsfeed.auth.jwt.service.JwtProvider;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.server.ServerHttpRequest;
import org.springframework.http.server.ServerHttpResponse;
import org.springframework.http.server.ServletServerHttpRequest;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.WebSocketHandler;
import org.springframework.web.socket.server.HandshakeInterceptor;

import java.util.Map;

/**
 * Validates the JWT presented during the WebSocket handshake.
 *
 * Clients pass the token as `?token=<jwt>` query param (browsers can't add
 * arbitrary headers to a WS handshake). On success we stash the memberId in
 * attributes, which {@link JwtHandshakeHandler} promotes to the STOMP Principal.
 */
@Slf4j
@Component
public class JwtHandshakeInterceptor implements HandshakeInterceptor {

    private final JwtProvider jwtProvider;

    public JwtHandshakeInterceptor(JwtProvider jwtProvider) {
        this.jwtProvider = jwtProvider;
    }

    @Override
    public boolean beforeHandshake(ServerHttpRequest request, ServerHttpResponse response,
                                   WebSocketHandler wsHandler, Map<String, Object> attributes) {
        String token = extractToken(request);
        if (token == null || !jwtProvider.validateToken(token)) {
            log.warn("WS handshake rejected — invalid/missing token");
            return false;
        }
        Long memberId = jwtProvider.getMemberId(token);
        attributes.put("memberId", memberId);
        attributes.put("email", jwtProvider.getUsername(token));
        return true;
    }

    @Override
    public void afterHandshake(ServerHttpRequest request, ServerHttpResponse response,
                               WebSocketHandler wsHandler, Exception ex) {}

    private String extractToken(ServerHttpRequest request) {
        if (request instanceof ServletServerHttpRequest servlet) {
            String header = servlet.getServletRequest().getHeader("Authorization");
            if (header != null && header.startsWith("Bearer ")) return header.substring(7);
            String q = servlet.getServletRequest().getParameter("token");
            if (q != null && !q.isBlank()) return q;
        }
        return null;
    }
}
