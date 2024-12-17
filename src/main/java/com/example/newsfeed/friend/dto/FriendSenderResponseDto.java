package com.example.newsfeed.friend.dto;

import lombok.Getter;

@Getter
public class FriendSenderResponseDto {

    private String senderEmail;
    private String senderName;
    private String receiverName;    // 받는 사람의 이름
    private String receiverEmail;   // 받는 사람의 이메일

    public FriendSenderResponseDto(String senderEmail, String senderName, String receiverName, String receiverEmail) {
        this.senderEmail = senderEmail;
        this.senderName = senderName;
        this.receiverName = receiverName;
        this.receiverEmail = receiverEmail;
    }
}
