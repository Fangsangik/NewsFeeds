package com.example.newsfeed.message.dto;

import com.example.newsfeed.message.type.MessageType;
import lombok.Getter;

@Getter
public class MessagePayload {
    private Long senderId;
    private Long receiverId;
    private String content;
    private MessageType type; // 메시지 유형, 예: "CHAT"

    public MessagePayload(Long senderId, Long receiverId, String content, MessageType type) {
        this.senderId = senderId;
        this.receiverId = receiverId;
        this.content = content;
        this.type = type;
    }

    public MessagePayload() {
    }
}
