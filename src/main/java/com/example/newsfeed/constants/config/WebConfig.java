package com.example.newsfeed.constants.config;

import com.example.newsfeed.auth.jwt.filter.JwtFilter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.access.hierarchicalroles.RoleHierarchy;
import org.springframework.security.access.hierarchicalroles.RoleHierarchyImpl;
import org.springframework.security.authentication.AuthenticationProvider;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.access.AccessDeniedHandler;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

@Configuration
@EnableWebSecurity
public class WebConfig {
    private final JwtFilter jwtFilter;
    private final AuthenticationProvider authenticationProvider;
    private final AuthenticationEntryPoint authenticationEntryPoint;
    private final AccessDeniedHandler accessDeniedHandler;

    private static final String[] WHITELIST = {
            "/ouaht/**", "/kakao/**"};

    public WebConfig(JwtFilter jwtFilter, AuthenticationProvider authenticationProvider, AuthenticationEntryPoint authenticationEntryPoint, AccessDeniedHandler accessDeniedHandler) {
        this.jwtFilter = jwtFilter;
        this.authenticationProvider = authenticationProvider;
        this.authenticationEntryPoint = authenticationEntryPoint;
        this.accessDeniedHandler = accessDeniedHandler;
    }

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
                .cors(AbstractHttpConfigurer::disable) // CORS 비활성화
                .csrf(AbstractHttpConfigurer::disable) // CSRF 비활성화
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers(WHITELIST).permitAll() // 화이트리스트 URL 허용
                        .requestMatchers("/admin/**").hasRole("ADMIN") // ADMIN 역할 필요
                        .requestMatchers("/user/**").hasRole("USER") // USER 역할 필요
                        .anyRequest().authenticated() // 나머지는 인증 필요
                )
                .exceptionHandling(handler -> handler
                        .authenticationEntryPoint(authenticationEntryPoint) // 인증 실패 처리
                        .accessDeniedHandler(accessDeniedHandler) // 권한 부족 처리
                )
                .sessionManagement(session -> session
                        .sessionCreationPolicy(SessionCreationPolicy.STATELESS) // JWT를 사용하므로 STATELESS 설정
                )
                .authenticationProvider(authenticationProvider) // 사용자 정의 AuthenticationProvider 추가
                .addFilterBefore(jwtFilter, UsernamePasswordAuthenticationFilter.class); // JWT 필터 추가

        return http.build();
    }

    /**
     * 사용자 권한 계층 설정
     * @return RoleHierarchy
     */
    @Bean
    public RoleHierarchy roleHierarchy() {
        return RoleHierarchyImpl.fromHierarchy("ROLE_ADMIN > ROLE_USER");
    }
}
