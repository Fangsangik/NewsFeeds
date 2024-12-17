package com.example.newsfeed.friend.type;


import com.example.newsfeed.friend.entity.Friend;

public enum FriendRequestStatus {
    REQUESTED("요청됨") {
        @Override
        public void accept(Friend friend) {
            friend.setStatus(ACCEPTED);
        }
    },
    ACCEPTED("수락됨"){
        @Override
        public void accept(Friend friend) {
            throw new IllegalArgumentException("이미 수락된 요청입니다.");
        }
    },
    REJECTED("거절됨"){
        @Override
        public void accept(Friend friend) {
            throw new IllegalArgumentException("거절된 요청입니다.");
        }
    };

    private final String message;

    FriendRequestStatus(String message) {
        this.message = message;
    }

    public void accept(Friend friend) {
        throw new IllegalArgumentException("수락할 수 없는 상태입니다.");
    }
}
