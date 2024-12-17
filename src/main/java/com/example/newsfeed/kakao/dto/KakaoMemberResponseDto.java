package com.example.newsfeed.kakao.dto;

import com.example.newsfeed.member.entity.Member;
import lombok.Getter;

@Getter
public class KakaoMemberResponseDto {
    private Long id;
    private String name;
    private String email;

    public KakaoMemberResponseDto(Member member) {
        this.id = member.getId();
        this.name = member.getName();
        this.email = member.getEmail();
    }

    public static KakaoMemberResponseDto fromEntity(Member member) {
        return new KakaoMemberResponseDto(member);
    }
}
