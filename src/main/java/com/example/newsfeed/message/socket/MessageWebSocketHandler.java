package com.example.newsfeed.message.socket;

import com.example.newsfeed.auth.jwt.service.JwtProvider;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.CloseStatus;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;
import org.springframework.web.socket.handler.TextWebSocketHandler;

import java.io.IOException;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Slf4j
@Component
@RequiredArgsConstructor
public class MessageWebSocketHandler extends TextWebSocketHandler {
    private final MessagePublisher messagePublisher;
    private final JwtProvider jwtTokenProvider;
    private final Map<String, WebSocketSession> sessions = new ConcurrentHashMap<>();

    /**
     * WebSocket 연결시 해당 세션을 저장
     * @param session
     */
    @Override
    public void afterConnectionEstablished(WebSocketSession session) {
        log.info("✅ WebSocket 연결됨: {}", session.getId());

        // ✅ WebSocket 세션에서 JWT 토큰 검증
        Map<String, Object> attributes = session.getAttributes();
        String token = (String) attributes.get("Authorization");

        if (token == null || !jwtTokenProvider.validateToken(token)) {
            log.error("❌ 인증 실패: JWT 토큰이 유효하지 않음");
            try {
                session.close(CloseStatus.NOT_ACCEPTABLE);
            } catch (Exception e) {
                log.error("🚨 WebSocket 종료 실패: {}", e.getMessage());
            }
            return;
        }

        log.info("🔑 JWT 인증 성공: 사용자 인증 완료");
    }

    /**
     * 클라이언트가 메시지를 보내면 로그를 남김
     * @param session
     * @param message
     * @throws IOException
     */
    @Override
    protected void handleTextMessage(WebSocketSession session, TextMessage message) throws IOException {
        log.info("📩 메시지 수신: {}", message.getPayload());

        String payload = message.getPayload();
        if ("ping".equalsIgnoreCase(payload)) {
            session.sendMessage(new TextMessage("pong"));
            log.info("✅ Ping 요청에 대한 Pong 응답 전송");
            return;
        }

        messagePublisher.publish("chat", payload);
    }

    /**
     * 해당 WebSocket 종료시 해당 세션을 삭제
     * @param session
     * @param status
     */
    @Override
    public void afterConnectionClosed(WebSocketSession session, CloseStatus status) {
        sessions.remove(session.getId());
        log.info("🚪 WebSocket 연결 종료됨: 세션 ID: {}", session.getId());
    }

    @Override
    public void handleTransportError(WebSocketSession session, Throwable exception) {
        log.error("🚨 WebSocket 오류 발생: {}", exception.getMessage());
    }
}
