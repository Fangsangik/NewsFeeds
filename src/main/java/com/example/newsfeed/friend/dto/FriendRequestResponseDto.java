package com.example.newsfeed.friend.dto;

import lombok.Getter;

@Getter
public class FriendRequestResponseDto {

    private final String receiverEmail;
    private final String receiverName;

    public FriendRequestResponseDto(String receiverEmail, String receiverName) {
        this.receiverEmail = receiverEmail;
        this.receiverName = receiverName;
    }
}
