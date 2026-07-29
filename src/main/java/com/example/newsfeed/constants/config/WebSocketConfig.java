package com.example.newsfeed.constants.config;

import com.example.newsfeed.auth.jwt.filter.JwtHandshakeHandler;
import com.example.newsfeed.auth.jwt.filter.JwtHandshakeInterceptor;
import org.springframework.context.annotation.Configuration;
import org.springframework.messaging.simp.config.MessageBrokerRegistry;
import org.springframework.web.socket.config.annotation.EnableWebSocketMessageBroker;
import org.springframework.web.socket.config.annotation.StompEndpointRegistry;
import org.springframework.web.socket.config.annotation.WebSocketMessageBrokerConfigurer;

@Configuration
@EnableWebSocketMessageBroker
public class WebSocketConfig implements WebSocketMessageBrokerConfigurer {

    private final JwtHandshakeInterceptor handshakeInterceptor;
    private final JwtHandshakeHandler handshakeHandler;

    public WebSocketConfig(JwtHandshakeInterceptor handshakeInterceptor,
                           JwtHandshakeHandler handshakeHandler) {
        this.handshakeInterceptor = handshakeInterceptor;
        this.handshakeHandler = handshakeHandler;
    }

    @Override
    public void registerStompEndpoints(StompEndpointRegistry registry) {
        registry.addEndpoint("/ws")
                .setAllowedOriginPatterns("*")
                .setHandshakeHandler(handshakeHandler)
                .addInterceptors(handshakeInterceptor);
        // Raw WebSocket only — SockJS's xhr_streaming fallback strips the
        // ?token= from sub-URLs, so we keep handshake auth via the WS URL.
    }

    @Override
    public void configureMessageBroker(MessageBrokerRegistry registry) {
        // Broker forwards messages destined for /topic (broadcast) and /queue (user-specific) to clients.
        registry.enableSimpleBroker("/topic", "/queue");
        // Client publishes go to /app/... and are routed to @MessageMapping handlers.
        registry.setApplicationDestinationPrefixes("/app");
        // Personal destinations resolve to /user/{principal}/queue/...
        registry.setUserDestinationPrefix("/user");
    }
}
