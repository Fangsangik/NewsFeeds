package com.example.newsfeed.message.config;

import com.example.newsfeed.auth.jwt.service.JwtProvider;
import com.example.newsfeed.interceptor.JwtHandShakeInterceptor;
import com.example.newsfeed.message.repository.MessageRepository;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.messaging.simp.config.MessageBrokerRegistry;
import org.springframework.web.socket.WebSocketSession;
import org.springframework.web.socket.config.annotation.*;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Configuration
//@EnableWebSocket
@EnableWebSocketMessageBroker // STOMP 메시징을 사용하기 위한 어노테이션
public class WebSocketConfig implements WebSocketMessageBrokerConfigurer {

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

    //웹 소켓을 연결을 위해 사용할 엔드포인트 & CORS 오류 방지 설정
    @Override
    public void registerStompEndpoints(StompEndpointRegistry registry) {
        registry.addEndpoint("/ws-stomp")
                .setAllowedOriginPatterns("*") // origin 허용
                .addInterceptors(new JwtHandShakeInterceptor(jwtProvider))
                .withSockJS(); // WebSocket을 지원하지 않는 환경에서도 사용 가능
    }

    //친구 설정시 사용할 메시지 브로커 설정
    @Override
    public void configureMessageBroker(MessageBrokerRegistry registry) {
        //"sub"가 prefix인 destination을 가진 메시지를 브로커로 라우팅 설정
        registry.enableSimpleBroker("/sub");
        //"/pub"가 prefix로 붙은 메시지를 @Controller내에서 @MessageMapping이 붙은 메소드로 라우탕
        registry.setApplicationDestinationPrefixes("/pub");
    }
}
