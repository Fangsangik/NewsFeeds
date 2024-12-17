package com.example.newsfeed.config;

import lombok.extern.slf4j.Slf4j;
import org.springframework.web.socket.CloseStatus;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;
import org.springframework.web.socket.handler.TextWebSocketHandler;

import java.util.concurrent.ConcurrentHashMap;

@Slf4j
public class MyWebSocketHandler extends TextWebSocketHandler {

    private static final ConcurrentHashMap<Long, WebSocketSession> userSessions = new ConcurrentHashMap<>();

    // 클라이언트 접속 시
    @Override
    public void afterConnectionEstablished(WebSocketSession session) throws Exception {
        Long memberId = getMemberId(session);

        if (memberId != null) {
            userSessions.put(memberId, session);
            log.info("클라이언트 접속: {}", memberId, session.getId());
        } else {
            session.sendMessage(new TextMessage("로그인이 필요합니다."));
            session.close();
        }
    }

    // 클라이언트로부터 메시지 수신 시

    @Override
    protected void handleTextMessage(WebSocketSession session, TextMessage message) throws Exception {
        Long memberId = getMemberId(session);
        log.info("클라이언트로부터 메시지 수신: {}", memberId, message.getPayload());
    }

    // 클라이언트 접속 해제 시
    @Override
    public void afterConnectionClosed(WebSocketSession session, CloseStatus status) throws Exception {
        Long memberId = getMemberId(session);
        if (memberId != null) {
            userSessions.remove(memberId);
        }
        log.info("클라이언트 접속 해제: {}", session.getId());
    }

    @Override
    public void handleTransportError(WebSocketSession session, Throwable exception) throws Exception {
        Long memberId = getMemberId(session);
        log.error("클라이언트 {}와 통신 중 에러 발생: {}", memberId, exception.getMessage());
    }

    public void sendMessageToUser(Long receiverId, String message) {
        WebSocketSession receiverSession = userSessions.get(receiverId);
        if (receiverSession != null && receiverSession.isOpen()) {
            try {
                receiverSession.sendMessage(new TextMessage(message));
                log.info("메시지 전송 성공: {}", message);
            } catch (Exception e) {
                log.error("메시지 전송 실패: {}", e.getMessage());
            }
        } else {
            log.warn("WebSocket 세션이 존재하지 않음: 사용자 ID {}", receiverId);
        }
    }

    private static Long getMemberId(WebSocketSession session) {
        return (Long) session.getAttributes().get("memberId");
    }
}
