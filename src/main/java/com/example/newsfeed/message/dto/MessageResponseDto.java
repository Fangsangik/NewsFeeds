package com.example.newsfeed.message.dto;

import com.example.newsfeed.message.entity.Message;
import lombok.Builder;
import lombok.Getter;

import java.time.LocalDateTime;

@Getter
@Builder
public class MessageResponseDto {
    private Long id;
    private Long senderId;
    private Long receiverId;
    private String message;
    private boolean readStatus;
    private LocalDateTime createdAt;

    public static MessageResponseDto from(Message m) {
        return MessageResponseDto.builder()
                .id(m.getId())
                .senderId(m.getSender() != null ? m.getSender().getId() : null)
                .receiverId(m.getReceiver() != null ? m.getReceiver().getId() : null)
                .message(m.getMessage())
                .readStatus(m.isReadStatus())
                .createdAt(m.getCreatedAt())
                .build();
    }
}
