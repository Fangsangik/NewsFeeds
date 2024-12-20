package com.example.newsfeed.interceptor;

import com.example.newsfeed.auth.jwt.service.JwtProvider;
import com.example.newsfeed.member.type.Role;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.stereotype.Component;

@Component
public class AdminInterceptor extends BaseInterceptor {

    public AdminInterceptor(JwtProvider jwtProvider) {
        super(jwtProvider);
    }

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) throws Exception {
        return validateTokenAndRole(request, response, Role.ADMIN);
    }
}
