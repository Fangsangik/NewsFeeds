package com.example.newsfeed.config;

import com.example.newsfeed.auth.jwt.JwtProvider;
import com.example.newsfeed.interceptor.JwtHandShakeInterceptor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.socket.config.annotation.EnableWebSocket;
import org.springframework.web.socket.config.annotation.WebSocketConfigurer;
import org.springframework.web.socket.config.annotation.WebSocketHandlerRegistry;

@Configuration
@EnableWebSocket
public class WebSocketConfig implements WebSocketConfigurer {

    private final JwtProvider jwtProvider;

    public WebSocketConfig(JwtProvider jwtProvider) {
        this.jwtProvider = jwtProvider;
    }

    @Bean
    public MyWebSocketHandler myWebSocketHandler() {
        return new MyWebSocketHandler();
    }

    @Override
    public void registerWebSocketHandlers(WebSocketHandlerRegistry registry) {
        // /ws/chat 경로로 들어오는 요청을 처리하는 핸들러를 등록
        registry.addHandler(new MyWebSocketHandler(), "/ws/chat")
                .addInterceptors(new JwtHandShakeInterceptor(jwtProvider))
                .setAllowedOrigins("*"); //CORS 허용
    }
}
