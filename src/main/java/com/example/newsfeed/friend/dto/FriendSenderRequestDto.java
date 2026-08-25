package com.example.newsfeed.friend.dto;

import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
public class FriendSenderRequestDto {

    // 보낸 요청 목록에서 '요청을 받은 상대'를 담는다. 프런트(friends.js sent 탭)가
    // receiverName/receiverEmail 를 읽으므로 필드명을 맞춘다.
    private String receiverEmail;
    private String receiverName;

    public FriendSenderRequestDto(String receiverEmail, String receiverName) {
        this.receiverEmail = receiverEmail;
        this.receiverName = receiverName;
    }
}
