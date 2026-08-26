package com.example.newsfeed.friend.dto;

import com.example.newsfeed.member.entity.Member;
import lombok.Getter;

/** 팔로우 추천(알 수도 있는 사람) 항목: 회원 + 공통 친구 수. */
@Getter
public class FriendSuggestionDto {
    private final Long id;
    private final String name;
    private final String image;
    private final long mutual;

    public FriendSuggestionDto(Member m, long mutual) {
        this.id = m.getId();
        this.name = m.getName();
        this.image = m.getImage();
        this.mutual = mutual;
    }
}
