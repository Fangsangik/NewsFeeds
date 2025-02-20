package com.example.newsfeed.friend.dto;

import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
public class FriendSenderRequestDto {

    private String senderEmail;
    private String senderName;

    public FriendSenderRequestDto(String senderEmail, String senderName) {
        this.senderEmail = senderEmail;
        this.senderName = senderName;
    }
}
