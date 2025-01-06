package com.example.newsfeed.message.service;

import com.example.newsfeed.message.dto.MessagePayload;
import com.example.newsfeed.message.type.MessageType;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;

/**
 * ChatMessageSubscriber
 * redis에서 수신한 메시지를 처리
 */
@Slf4j
@Service
public class ChatMessageSubscriber {

    private final ObjectMapper objectMapper;
    private final SimpMessagingTemplate simpMessagingTemplate;

    public ChatMessageSubscriber(ObjectMapper objectMapper, SimpMessagingTemplate simpMessagingTemplate) {
        this.objectMapper = objectMapper;
        this.simpMessagingTemplate = simpMessagingTemplate;
    }

    @PostConstruct
    public void init() {
        log.info("ChatMessageSubscriber 시작");
    }

    // RedisMessageListenerContainer에 의해 메시지를 수신
    public void handleMessage(String message) {
        log.info("Redis에서 수신된 메시지: {}", message);
        try {
            // 메시지 처리 로직 추가
            MessagePayload payload = objectMapper.readValue(message, MessagePayload.class);

            if (MessageType.CHAT.equals(payload.getType())) {
                simpMessagingTemplate.convertAndSend("/sub/chat/" + payload.getReceiverId(), payload.getContent()
                );
            } else {
                log.error("지원하지 않는 메시지 유형: {}", payload.getType());
            }
        } catch (Exception e) {
            log.error("메시지 처리 중 오류: {}", e.getMessage());
        }
    }
}
