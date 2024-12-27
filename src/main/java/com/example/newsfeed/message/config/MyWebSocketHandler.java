package com.example.newsfeed.message.config;

import com.example.newsfeed.message.entity.Message;
import com.example.newsfeed.message.repository.MessageRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.socket.CloseStatus;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;
import org.springframework.web.socket.handler.TextWebSocketHandler;

import java.util.List;
import java.util.concurrent.ConcurrentHashMap;

@Slf4j
public class MyWebSocketHandler extends TextWebSocketHandler {

    private final MessageRepository messageRepository;

    private static final ConcurrentHashMap<Long, WebSocketSession> userSessions = new ConcurrentHashMap<>();

    public MyWebSocketHandler(MessageRepository messageRepository) {
        this.messageRepository = messageRepository;
    }

    // 클라이언트 접속 시
    @Override
    public void afterConnectionEstablished(WebSocketSession session) throws Exception {
        Long memberId = getMemberId(session);

        if (memberId != null) {
            userSessions.put(memberId, session);
            log.info("클라이언트 접속: {}", memberId);

            List<Message> unreadMessages = messageRepository.findByReceiverIdAndReadStatusFalse(memberId);
            for (Message unreadMessage : unreadMessages) {
                session.sendMessage(new TextMessage(unreadMessage.getMessage()));
                unreadMessage.setReadStatus(true);
            }
            messageRepository.saveAll(unreadMessages);
        } else {
            session.sendMessage(new TextMessage("로그인이 필요합니다."));
            session.close();
        }
    }

    // 클라이언트로부터 메시지 수신 시

    @Override
    protected void handleTextMessage(WebSocketSession session, TextMessage message) throws Exception {
        Long memberId = getMemberId(session);
        log.info("메시지 수신: 사용자 ID={}, 메시지={}", memberId, message.getPayload());
    }

    // 클라이언트 접속 해제 시
    @Override
    public void afterConnectionClosed(WebSocketSession session, CloseStatus status)  {
        Long memberId = getMemberId(session);
        if (memberId != null) {
            userSessions.remove(memberId);
        }
        log.info("클라이언트 접속 해제: {}", session.getId());
    }

    // 클라이언트에게 메시지 전송
    public void senMessage(WebSocketSession session, String message) {
       if (session.isOpen()) {
           try {
               session.sendMessage(new TextMessage(message));
           } catch (Exception e) {
               log.error("메시지 전송 오류: {}", e.getMessage());
           }
       }
    }

    private static Long getMemberId(WebSocketSession session) {
        return (Long) session.getAttributes().get("memberId");
    }
}
