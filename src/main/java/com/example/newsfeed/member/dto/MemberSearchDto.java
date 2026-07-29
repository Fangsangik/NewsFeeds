package com.example.newsfeed.member.dto;

import com.example.newsfeed.member.entity.Member;
import lombok.Getter;

@Getter
public class MemberSearchDto {
    private final Long id;
    private final String name;
    private final String email;
    private final String image;

    public MemberSearchDto(Long id, String name, String email, String image) {
        this.id = id;
        this.name = name;
        this.email = email;
        this.image = image;
    }

    public static MemberSearchDto from(Member m) {
        return new MemberSearchDto(m.getId(), m.getName(), m.getEmail(), m.getImage());
    }
}
