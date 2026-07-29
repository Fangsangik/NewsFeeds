package com.example.newsfeed.auth.jwt.filter;

import com.example.newsfeed.auth.jwt.service.JwtProvider;
import com.example.newsfeed.auth.jwt.service.UserDetailsImpl;
import com.example.newsfeed.member.entity.Member;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

@Slf4j
@Component
//request마다 처리하는 JWT 기반 인증 필터.
public class JwtFilter extends OncePerRequestFilter {

    private final JwtProvider jwtProvider;

    public JwtFilter(JwtProvider jwtProvider) {
        this.jwtProvider = jwtProvider;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain) throws ServletException, IOException {
        log.info("URI : {}", request.getRequestURI());
        this.authenticate(request);
        filterChain.doFilter(request, response);
    }

    /**
     * request를 이용해 인증 처리
     *
     * @param request
     */
    private void authenticate(HttpServletRequest request) {
        log.info("인가 처리");

        String token = this.getTokenFromRequest(request);
        if (!jwtProvider.validateToken(token)) {
            return;
        }

        // 서명이 검증된 토큰의 claims만으로 인증 주체를 구성한다 (매 요청 DB 조회 제거).
        // id/email/role은 access token 발급 시 심어둔 값이라 재조회가 필요 없다.
        Member principal = Member.fromClaims(
                jwtProvider.getMemberId(token),
                jwtProvider.getUsername(token),
                jwtProvider.getRoleFromToken(token));
        UserDetailsImpl userDetails = new UserDetailsImpl(principal);

        //SecurityContext에 인증 객체 저장
        this.setAuthentication(request, userDetails);
    }

    /**
     * request의 Authorization 헤더에서 토큰 추출
     * @param request
     * @return
     */
    private String getTokenFromRequest(HttpServletRequest request) {
        String bearer = request.getHeader("Authorization");
        if (bearer != null && bearer.startsWith("Bearer ")) {
            return bearer.substring(7);
        }
        return null;
    }

    /**
     * SecurityContext에 인증 객체 저장
     * @param request
     * @param userDetails
     */
    private void setAuthentication(HttpServletRequest request, UserDetails userDetails) {
        log.info("토큰 검증");

        UsernamePasswordAuthenticationToken auth = new UsernamePasswordAuthenticationToken(
                userDetails, userDetails.getPassword(), userDetails.getAuthorities());

        auth.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));

        SecurityContextHolder.getContext().setAuthentication(auth);
    }
}
