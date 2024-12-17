package com.example.newsfeed.kakao.dto;

import com.example.newsfeed.kakao.entity.KakaoMember;
import lombok.Builder;
import lombok.Getter;

@Getter
public class KakaoMemberRequestDto {
    private Long kakaoId;
    private String email;
    private String nickname;

    @Builder
    public KakaoMemberRequestDto(Long kakaoId, String email, String nickname) {
        this.kakaoId = kakaoId;
        this.email = email;
        this.nickname = nickname;
    }

    public static KakaoMember toEntity(KakaoMemberRequestDto requestDto) {
        return KakaoMember.builder()
                .kakaoId(requestDto.getKakaoId())
                .email(requestDto.getEmail())
                .nickname(requestDto.getNickname())
                .build();
    }
}
