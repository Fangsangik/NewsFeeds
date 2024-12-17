package com.example.newsfeed.member.dto;

import com.example.newsfeed.member.entity.Member;
import lombok.Getter;

@Getter
public class MemberResponseDto {
    private Long id;


    public MemberResponseDto(Member member) {
        this.id = member.getId();
    }

    public static MemberResponseDto toDto(Member member) {
        return new MemberResponseDto(member);
    }
}
