package com.example.newsfeed.friend.dto;

import com.example.newsfeed.friend.entity.Friend;
import com.example.newsfeed.friend.type.FriendRequestStatus;
import com.example.newsfeed.member.entity.Member;
import lombok.Getter;

@Getter
public class FriendRequestDto {
    private Long senderId;
    private Long receiverId;

    public FriendRequestDto(Long senderId, Long receiverId) {
        this.senderId = senderId;
        this.receiverId = receiverId;
    }

    public static Friend createFriend(Member sender, Member receiver) {
        return Friend.builder()
                .sender(sender)
                .receiver(receiver)
                .status(FriendRequestStatus.REQUESTED)
                .build();
    }
}
