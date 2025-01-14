package com.example.newsfeed.auth.jwt.filter;

import com.example.newsfeed.auth.jwt.service.JwtProvider;
import com.example.newsfeed.auth.jwt.service.UserDetailsImpl;
import com.example.newsfeed.exception.ErrorCode;
import com.example.newsfeed.exception.NoAuthorizedException;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

@Slf4j
@Component
//request마다 처리하는 JWT 기반 인증 필터.
public class JwtFilter extends OncePerRequestFilter {

    private final JwtProvider jwtProvider;
    private final UserDetailsService userDetailsService;


    public JwtFilter(JwtProvider jwtProvider, UserDetailsService userDetailsService) {
        this.jwtProvider = jwtProvider;
        this.userDetailsService = userDetailsService;
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

        //토큰에서 userName을 추출
        String username = this.jwtProvider.getUsername(token);
        Long memberIdFromToken = this.jwtProvider.getMemberId(token);

        //username에 해당하는 사용자 찾음
        UserDetailsImpl userDetails = (UserDetailsImpl) userDetailsService.loadUserByUsername(username);

        // 해당 userId와 userDetails에 있는 id와 동일한지 확인
        if (!memberIdFromToken.equals(userDetails.getMemberId())) {
            throw new NoAuthorizedException(ErrorCode.NO_AUTHOR);
        }

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
