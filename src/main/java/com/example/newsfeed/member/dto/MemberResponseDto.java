package com.example.newsfeed.member.dto;

import com.example.newsfeed.member.entity.Member;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
public class MemberResponseDto {
    private Long id;
    private String name;
    private String email;
    private String image;


    public MemberResponseDto(Member member) {
        this.id = member.getId();
        this.name = member.getName();
        this.email = member.getEmail();
        this.image = member.getImage();
    }

    public static MemberResponseDto toDto(Member member) {
        return new MemberResponseDto(member);
    }
}
