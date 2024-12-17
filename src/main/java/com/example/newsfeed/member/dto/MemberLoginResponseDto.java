package com.example.newsfeed.member.dto;

import com.example.newsfeed.member.entity.Member;
import lombok.Builder;
import lombok.Getter;

import java.util.Map;

@Getter
public class MemberLoginResponseDto {
    private Long id;
    private String accessToken;
    private String refreshToken;

    @Builder
    public MemberLoginResponseDto(Long id, String accessToken, String refreshToken) {
        this.id = id;
        this.accessToken = accessToken;
        this.refreshToken = refreshToken;
    }

    public static MemberLoginResponseDto toDto(Member savedMember, Map<String, String> tokens) {
        return MemberLoginResponseDto.builder()
                .id(savedMember.getId())
                .accessToken(tokens.get("accessToken"))
                .refreshToken(tokens.get("refreshToken"))
                .build();
    }
}
