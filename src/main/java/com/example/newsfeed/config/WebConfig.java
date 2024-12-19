package com.example.newsfeed.config;

import com.example.newsfeed.interceptor.AdminInterceptor;
import com.example.newsfeed.interceptor.AuthInterceptor;
import com.example.newsfeed.interceptor.KakaoInterceptor;
import com.example.newsfeed.interceptor.UserInterceptor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.Ordered;
import org.springframework.web.filter.CharacterEncodingFilter;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
public class WebConfig implements WebMvcConfigurer {

    private static final String[] USER_ROLE_REQUIRED_PATH_PATTERNS = {"/feeds/*", "/comments/*", "/likes/*", "/messages/*"};
    private static final String ADMIN_ROLE_REQUIRED_PATH_PATTERN = "/admins/*";

    private static final String[] WHITE_LIST = {"/members/signup", "/error", "/ouath/*", "/kakao/*"};

    private final AuthInterceptor authInterceptor;

    private final KakaoInterceptor kakaoInterceptor;

    private final UserInterceptor userInterceptor;

    private final AdminInterceptor adminInterceptor;

    public WebConfig(AuthInterceptor authInterceptor, KakaoInterceptor kakaoInterceptor, UserInterceptor userInterceptor, AdminInterceptor adminInterceptor) {
        this.authInterceptor = authInterceptor;
        this.kakaoInterceptor = kakaoInterceptor;
        this.userInterceptor = userInterceptor;
        this.adminInterceptor = adminInterceptor;
    }


    /**
     * 인터셉터의 우선순위와 Path를 설정한다.
     *
     * @param registry
     */
    public void addInterceptors(InterceptorRegistry registry) {
        registry.addInterceptor(authInterceptor)
                .excludePathPatterns(WHITE_LIST)
                .addPathPatterns("/**") // 모든 경로 적용
                .order(Ordered.HIGHEST_PRECEDENCE);

        registry.addInterceptor(userInterceptor)
                .excludePathPatterns(WHITE_LIST)
                .addPathPatterns(USER_ROLE_REQUIRED_PATH_PATTERNS)
                .order(Ordered.HIGHEST_PRECEDENCE + 1);

        registry.addInterceptor(kakaoInterceptor)
                .excludePathPatterns(WHITE_LIST)
                .addPathPatterns("/**")
                .order(Ordered.HIGHEST_PRECEDENCE + 2);

        registry.addInterceptor(adminInterceptor)
                .excludePathPatterns(WHITE_LIST)
                .addPathPatterns(ADMIN_ROLE_REQUIRED_PATH_PATTERN)
                .order(Ordered.HIGHEST_PRECEDENCE + 3);
    }

    @Bean
    public CharacterEncodingFilter characterEncodingFilter() {
        CharacterEncodingFilter filter = new CharacterEncodingFilter();
        filter.setEncoding("UTF-8");
        filter.setForceEncoding(true);
        return filter;
    }
}
