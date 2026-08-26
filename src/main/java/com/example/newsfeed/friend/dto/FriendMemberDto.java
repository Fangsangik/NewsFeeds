package com.example.newsfeed.friend.dto;

import com.example.newsfeed.member.entity.Member;
import lombok.Getter;

/** 팔로워/팔로잉 목록 항목 (프로필 이동용). */
@Getter
public class FriendMemberDto {
    private final Long id;
    private final String name;
    private final String image;

    public FriendMemberDto(Member m) {
        this.id = m.getId();
        this.name = m.getName();
        this.image = m.getImage();
    }
}
