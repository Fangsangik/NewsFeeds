package com.example.newsfeed.friend.dto;

import com.example.newsfeed.friend.entity.Friend;
import com.example.newsfeed.friend.type.FriendRequestStatus;
import com.example.newsfeed.member.entity.Member;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
public class FriendRequestDto {
    private Long receiverId;

    public FriendRequestDto(Long receiverId) {
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
