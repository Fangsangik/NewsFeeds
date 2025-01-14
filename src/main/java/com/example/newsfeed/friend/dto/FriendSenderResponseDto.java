package com.example.newsfeed.friend.dto;

import lombok.Getter;

@Getter
public class FriendSenderResponseDto {

    private final String senderEmail;
    private final String senderName;

    public FriendSenderResponseDto(String senderEmail, String senderName) {
        this.senderEmail = senderEmail;
        this.senderName = senderName;
    }
}
