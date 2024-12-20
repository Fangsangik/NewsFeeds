package com.example.newsfeed.interceptor;

import com.example.newsfeed.auth.jwt.service.JwtProvider;
import com.example.newsfeed.member.type.Role;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.web.servlet.HandlerInterceptor;

import java.io.IOException;

public abstract class BaseInterceptor implements HandlerInterceptor {

    protected final JwtProvider jwtProvider;

    protected BaseInterceptor(JwtProvider jwtProvider) {
        this.jwtProvider = jwtProvider;
    }

    protected boolean validateTokenAndRole(HttpServletRequest request, HttpServletResponse response, Role requiredRole) {
        String authHeader = request.getHeader("Authorization");

        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
            return false;
        }

        String token = authHeader.substring(7);

        if (!jwtProvider.validateToken(token)) {
            response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
            return false;
        }

        Role role = jwtProvider.getRoleFromToken(token);
        if (role != requiredRole) {
            response.setStatus(HttpServletResponse.SC_FORBIDDEN);
            return false;
        }

        return true;
    }
}
