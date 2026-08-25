package com.example.newsfeed.auth.jwt.filter;

import org.springframework.http.server.ServerHttpRequest;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.WebSocketHandler;
import org.springframework.web.socket.server.support.DefaultHandshakeHandler;

import java.security.Principal;
import java.util.Map;

/**
 * Promotes the memberId that {@link JwtHandshakeInterceptor} stashed in
 * handshake attributes into a STOMP Principal whose name is the memberId
 * as a string. This is what lets us address users via
 * {@code SimpMessagingTemplate.convertAndSendToUser(memberId, ...)}.
 */
@Component
public class JwtHandshakeHandler extends DefaultHandshakeHandler {

    @Override
    protected Principal determineUser(ServerHttpRequest request,
                                      WebSocketHandler wsHandler,
                                      Map<String, Object> attributes) {
        Object memberId = attributes.get("memberId");
        if (memberId == null) return super.determineUser(request, wsHandler, attributes);
        final String name = String.valueOf(memberId);
        return () -> name;
    }
}
