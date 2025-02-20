package com.example.newsfeed.friend.dto;

import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
public class FriendAcceptRequestDto {
    private Long senderId;

    public FriendAcceptRequestDto(Long senderId) {
        this.senderId = senderId;
    }
}
