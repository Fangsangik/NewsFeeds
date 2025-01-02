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
        log.info("메시지 수신: {}", message);

        try {
            MessagePayload payload = new ObjectMapper().readValue(message, MessagePayload.class);

            if (MessageType.CHAT.equals(payload.getType())) {
                simpMessagingTemplate.convertAndSend("/sub/chat/room/" + payload.getReceiverId(), payload);
            } else {
                log.error("알 수 없는 메시지: {}", message);
            }

        } catch (Exception e) {
            log.error("메시지 처리 중 오류 발생: {}", e.getMessage());
        }
    }
}
