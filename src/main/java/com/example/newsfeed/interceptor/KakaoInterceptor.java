package com.example.newsfeed.interceptor;

import com.example.newsfeed.auth.jwt.service.JwtProvider;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

@Component
public class KakaoInterceptor implements HandlerInterceptor {

    private final JwtProvider jwtProvider;

    public KakaoInterceptor(JwtProvider jwtProvider) {
        this.jwtProvider = jwtProvider;
    }

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) throws Exception {
        String token = getTokenFromRequest(request);
        String uri = request.getRequestURI();

        // 예외 경로 설정
        if (uri.equals("/members/signup") || uri.equals("/error") || uri.equals("/callback")) {
            return true; // 인터셉터 무시
        }

        if (token == null || !jwtProvider.validateToken(token)) {
            response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
            response.getWriter().write("유효하지 않은 토큰입니다.");
            return false;
        }

        Long memberId = jwtProvider.getUserIdFromToken(token);
        request.setAttribute("memberId", memberId);

        return true;
    }

    private String getTokenFromRequest(HttpServletRequest request) {
        String bearerToken = request.getHeader("Authorization");
        if (bearerToken != null && bearerToken.startsWith("Bearer ")) {
            return bearerToken.substring(7);
        }
        return null;
    }
}
