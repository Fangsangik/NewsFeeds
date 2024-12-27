package com.example.newsfeed.message.controller;

import com.example.newsfeed.message.service.ChatMessagePublisher;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/chat")
public class ChatController {

    public final ChatMessagePublisher chatMessagePublisher;

    public ChatController(ChatMessagePublisher chatMessagePublisher) {
        this.chatMessagePublisher = chatMessagePublisher;
    }

    @MessageMapping("/send")
    public void sendMessage(String message) {
        //Redis 체널에서 메시지 발행
        chatMessagePublisher.publish("chat", message);
    }

    /**
     * sub의 경우 Spring webSocket 브로커 설정을 통해 Sub은 클라이언트가 자동으로 처리
     */
}
