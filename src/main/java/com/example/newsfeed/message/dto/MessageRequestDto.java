package com.example.newsfeed.message.dto;

import com.example.newsfeed.member.entity.Member;
import com.example.newsfeed.message.entity.Message;
import lombok.Getter;

import java.time.LocalDateTime;

@Getter
public class MessageRequestDto {

    private Long senderId;
    private Long receiverId;
    private String message;
    private LocalDateTime timeStamp;

    public MessageRequestDto(Long senderId, Long receiverId, String message, LocalDateTime timeStamp) {
        this.senderId = senderId;
        this.receiverId = receiverId;
        this.message = message;
        this.timeStamp = timeStamp;
    }

    public static Message toEntity(Member sender, Member receiver) {
        return Message.builder()
                .sender(sender)
                .receiver(receiver)
                .timestamp(LocalDateTime.now())
                .readStatus(false)
                .build();
    }
}
