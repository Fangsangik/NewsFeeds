package com.example.newsfeed.friend.dto;

import com.example.newsfeed.friend.entity.Friend;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
public class FriendResponseDto {

    private Long senderId;
    private String senderName;


    public FriendResponseDto(Long senderId, String senderName) {
        this.senderId = senderId;
        this.senderName = senderName;
    }

    public static FriendResponseDto toDto(Friend friend) {
        return new FriendResponseDto(friend.getSender().getId(), friend.getReceiver().getName());
    }
}
