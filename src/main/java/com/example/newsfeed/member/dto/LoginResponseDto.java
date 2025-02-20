package com.example.newsfeed.member.dto;

import com.example.newsfeed.member.entity.Member;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.Map;

@Getter
@NoArgsConstructor
public class LoginResponseDto {
    private Long id;
    private String accessToken;
    private String refreshToken;

    @Builder
    public LoginResponseDto(Long id, String accessToken, String refreshToken) {
        this.id = id;
        this.accessToken = accessToken;
        this.refreshToken = refreshToken;
    }

    public static LoginResponseDto toDto(Member savedMember, Map<String, String> tokens) {
        return LoginResponseDto.builder()
                .id(savedMember.getId())
                .accessToken(tokens.get("accessToken"))
                .refreshToken(tokens.get("refreshToken"))
                .build();
    }
}
