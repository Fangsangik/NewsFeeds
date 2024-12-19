package com.example.newsfeed.message.dto;

import lombok.Builder;
import lombok.Getter;

import java.time.LocalDateTime;

@Getter
public class MessageResponseDto {
    private String message;
    private Long senderId;
    private Long receiverId;
    private boolean readStatus;
    private LocalDateTime createdAt;

    @Builder
    public MessageResponseDto(String message, Long senderId, Long receiverId, boolean readStatus, LocalDateTime createdAt) {
        this.message = message;
        this.senderId = senderId;
        this.receiverId = receiverId;
        this.readStatus = readStatus;
        this.createdAt = createdAt;
    }

    public static MessageResponseDto toDto(String message, Long senderId, Long receiverId, boolean readStatus, LocalDateTime createdAt) {
        return MessageResponseDto.builder()
                .message(message)
                .senderId(senderId)
                .receiverId(receiverId)
                .readStatus(readStatus)
                .createdAt(createdAt)
                .build();
    }
}
