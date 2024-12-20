package com.example.newsfeed.auth.jwt.entity;

import com.example.newsfeed.member.entity.Member;
import jakarta.persistence.*;
import lombok.Builder;
import lombok.Getter;

import java.util.Date;

@Entity
@Getter
public class JwtToken {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String accessToken;
    private String refreshToken;

    private Date issuedAt;
    private Date expiredAt;

    private boolean isValid = true;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "member_id")
    private Member member;

    @Builder
    public JwtToken(Member member, Date issuedAt, String refreshToken, String accessToken, Date expiredAt) {
        this.member = member;
        this.issuedAt = issuedAt;
        this.refreshToken = refreshToken;
        this.accessToken = accessToken;
        this.expiredAt = expiredAt;
    }

    public JwtToken() {

    }
}
