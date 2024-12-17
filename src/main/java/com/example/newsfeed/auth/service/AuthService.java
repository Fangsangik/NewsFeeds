package com.example.newsfeed.auth.service;

import com.example.newsfeed.auth.dto.AuthLoginDto;
import com.example.newsfeed.auth.dto.LoginRequestDto;
import com.example.newsfeed.auth.jwt.JwtProvider;
import com.example.newsfeed.auth.type.LoginType;
import com.example.newsfeed.member.entity.Member;
import com.example.newsfeed.member.repository.MemberRepository;
import com.example.newsfeed.member.type.Role;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class AuthService {

    private final MemberRepository memberRepository;
    private final JwtProvider jwtProvider;


    public AuthService(MemberRepository memberRepository, JwtProvider jwtProvider) {
        this.memberRepository = memberRepository;
        this.jwtProvider = jwtProvider;
    }

    @Transactional
    public AuthLoginDto login(LoginRequestDto requestDto) {
        Member member = memberRepository.findByEmail(requestDto.getEmail())
                .orElseThrow(() -> new IllegalArgumentException("Email does not exist"));

        if (member.getPassword().equals(requestDto.getPassword())) {
            throw new IllegalArgumentException("Password does not match");
        }

        jwtProvider.generateTokens(member.getId(), Role.USER, LoginType.NORMAL_USER);

        return AuthLoginDto.builder()
                .email(member.getEmail())
                .build();
    }

    public String refreshAccessToken(String refreshToken) {
        if (!jwtProvider.validateToken(refreshToken)) {
            throw new IllegalArgumentException("Invalid Refresh Token");
        }
        Long userId = jwtProvider.getUserIdFromToken(refreshToken);
        return jwtProvider.generateTokens(Long.valueOf(userId), Role.USER, LoginType.NORMAL_USER).get("accessToken");
    }
}
