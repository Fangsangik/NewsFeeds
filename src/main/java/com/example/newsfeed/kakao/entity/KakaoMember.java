package com.example.newsfeed.kakao.entity;

import com.example.newsfeed.member.entity.Member;
import jakarta.persistence.*;
import lombok.Builder;
import lombok.Getter;

@Entity
@Getter
public class KakaoMember {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private Long kakaoId;

    private String email;
    private String nickname;

    @OneToOne(mappedBy = "kakaoMember", cascade = CascadeType.ALL)
    private Member member;

    @Builder
    public KakaoMember(Long kakaoId, String email, String nickname) {
        this.kakaoId = kakaoId;
        this.email = email;
        this.nickname = nickname;
    }

    public KakaoMember() {

    }
}
