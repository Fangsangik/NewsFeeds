package com.example.newsfeed.constants.config;

import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.AuthenticationProvider;
import org.springframework.security.authentication.dao.DaoAuthenticationProvider;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;

@Slf4j
@Configuration
public class SecurityConfig {

    private final UserDetailsService userDetailsService;

    public SecurityConfig(UserDetailsService userDetailsService) {
        this.userDetailsService = userDetailsService;
    }

    @Bean
    public BCryptPasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    /**
     * AuthenticationManager(인증 관련 처리)
     */
    @Bean
    public AuthenticationManager authenticationManager(AuthenticationConfiguration configuration) throws Exception {
        log.info("AuthenticationManager 빈 등록");
        return configuration.getAuthenticationManager();
    }

    /**
     * AuthenticationProvider(인증 공급)
     */
    @Bean
    public AuthenticationProvider authenticationProvider() {
        DaoAuthenticationProvider authProvider = new DaoAuthenticationProvider();
        log.info("AuthenticationProvider 설정. 구현체: {}", authProvider.getClass().getSimpleName());

        log.info("UserDetailsService에 사용자 관리 위임. 구현체: {}",
                this.userDetailsService.getClass().getSimpleName());
        authProvider.setUserDetailsService(this.userDetailsService);

        log.info("PasswordEncoder에 암호 검증 위임. 구현체: {}",
                this.passwordEncoder().getClass().getSimpleName());
        authProvider.setPasswordEncoder(passwordEncoder());

        return authProvider;
    }
}

