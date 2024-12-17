package com.example.newsfeed.member.type;

import lombok.Getter;

@Getter
public enum Role {
    ADMIN("관리자"),
    USER("사용자");

    private String message;

    Role(String message) {
        this.message = message;
    }
}
