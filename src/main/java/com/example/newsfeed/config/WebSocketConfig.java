package com.example.newsfeed.config;

import com.example.newsfeed.auth.jwt.JwtProvider;
import com.example.newsfeed.interceptor.JwtHandShakeInterceptor;
import com.example.newsfeed.message.repository.MessageRepository;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.socket.WebSocketSession;
import org.springframework.web.socket.config.annotation.EnableWebSocket;
import org.springframework.web.socket.config.annotation.WebSocketConfigurer;
import org.springframework.web.socket.config.annotation.WebSocketHandlerRegistry;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Configuration
@EnableWebSocket
public class WebSocketConfig implements WebSocketConfigurer {

    private final MessageRepository messageRepository;
    private final JwtProvider jwtProvider;

    public WebSocketConfig(MessageRepository messageRepository, JwtProvider jwtProvider) {
        this.messageRepository = messageRepository;
        this.jwtProvider = jwtProvider;
    }


    @Bean
    public Map<Long, WebSocketSession> userSessions() {
        return new ConcurrentHashMap<>();
    }

    @Bean
    public MyWebSocketHandler myWebSocketHandler() {
        return new MyWebSocketHandler(messageRepository);
    }

    @Override
    public void registerWebSocketHandlers(WebSocketHandlerRegistry registry) {
        // /ws/chat 경로로 들어오는 요청을 처리하는 핸들러를 등록
        registry.addHandler(myWebSocketHandler(), "/ws/chat")
                .addInterceptors(new JwtHandShakeInterceptor(jwtProvider))
                .setAllowedOrigins("*"); //CORS 허용
    }
}
