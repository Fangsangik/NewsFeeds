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

    public MessageRequestDto(Long senderId, Long receiverId, String message) {
        this.senderId = senderId;
        this.receiverId = receiverId;
        this.message = message;
    }

    public static Message toEntity(Member sender, Member receiver) {
        return Message.builder()
                .sender(sender)
                .receiver(receiver)
                .readStatus(false)
                .build();
    }
}
