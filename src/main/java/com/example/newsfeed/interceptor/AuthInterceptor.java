package com.example.newsfeed.interceptor;

import com.example.newsfeed.auth.jwt.JwtProvider;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

@Component
public class AuthInterceptor implements HandlerInterceptor {

    private final JwtProvider jwtProvider;

    public AuthInterceptor(JwtProvider jwtProvider) {
        this.jwtProvider = jwtProvider;
    }

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) throws Exception {
        String uri = request.getRequestURI();
        System.out.println("AuthInterceptor - Request URI: " + uri);

        // 나머지 로직
        String authHeader = request.getHeader("Authorization");

        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
            response.getWriter().write("{\"message\": \"인증 토큰이 없습니다.\"}");
            return false;
        }

        String token = authHeader.substring(7);

        if (!jwtProvider.validateToken(token)) {
            response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
            response.getWriter().write("{\"message\": \"유효하지 않은 토큰입니다.\"}");
            return false;
        }

        Long userId = jwtProvider.getUserIdFromToken(token);

        request.setAttribute("memberId", userId);
        return true;
    }

}
