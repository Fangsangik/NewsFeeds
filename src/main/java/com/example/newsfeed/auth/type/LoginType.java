package com.example.newsfeed.auth.type;

import lombok.Getter;

@Getter
public enum LoginType {
    KAKAO_USER("카카오 사용자"),
    NORMAL_USER("로컬 사용자");

    private String message;

    LoginType(String message) {
        this.message = message;
    }
}
