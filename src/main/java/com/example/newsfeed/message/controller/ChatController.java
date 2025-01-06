package com.example.newsfeed.message.controller;

import com.example.newsfeed.message.dto.MessagePayload;
import com.example.newsfeed.message.service.ChatMessagePublisher;
import lombok.extern.slf4j.Slf4j;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.handler.annotation.SendTo;
import org.springframework.messaging.simp.SimpMessageHeaderAccessor;
import org.springframework.web.bind.annotation.RestController;

@Slf4j
@RestController
public class ChatController {

    public final ChatMessagePublisher chatMessagePublisher;

    public ChatController(ChatMessagePublisher chatMessagePublisher) {
        this.chatMessagePublisher = chatMessagePublisher;
    }

    @MessageMapping("/sendMessage")
    @SendTo("/sub/chat/room")
    public MessagePayload sendMessage(MessagePayload messagePayload, SimpMessageHeaderAccessor headerAccessor) {
        log.info("WebSocket으로 수신된 메시지: {}", messagePayload);
        chatMessagePublisher.publish("chat", messagePayload);
        return messagePayload;
    }

    /**
     * sub의 경우 Spring webSocket 브로커 설정을 통해 Sub은 클라이언트가 자동으로 처리
     */
}
