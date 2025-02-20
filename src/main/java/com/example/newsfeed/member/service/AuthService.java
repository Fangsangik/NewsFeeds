package com.example.newsfeed.member.service;

import com.example.newsfeed.member.dto.LoginRequestDto;
import com.example.newsfeed.member.dto.LoginResponseDto;
import com.example.newsfeed.auth.jwt.dto.JwtMemberDto;
import com.example.newsfeed.auth.jwt.entity.JwtToken;
import com.example.newsfeed.auth.jwt.repository.TokenRepository;
import com.example.newsfeed.auth.jwt.service.JwtProvider;
import com.example.newsfeed.member.type.LoginType;
import com.example.newsfeed.exception.ErrorCode;
import com.example.newsfeed.exception.InvalidInputException;
import com.example.newsfeed.exception.NoAuthorizedException;
import com.example.newsfeed.member.entity.Member;
import com.example.newsfeed.member.repository.MemberRepository;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.Date;
import java.util.Map;

@Service
public class AuthService {

    private final MemberRepository memberRepository;
    private final JwtProvider jwtProvider;
    private final PasswordEncoder passwordEncoder;
    private final TokenRepository tokenRepository;


    public AuthService(MemberRepository memberRepository, JwtProvider jwtProvider, PasswordEncoder passwordEncoder, TokenRepository tokenRepository) {
        this.memberRepository = memberRepository;
        this.jwtProvider = jwtProvider;
        this.passwordEncoder = passwordEncoder;
        this.tokenRepository = tokenRepository;
    }

    @Transactional
    public LoginResponseDto login(LoginRequestDto requestDto) {

        Member member = memberRepository.findByEmail(requestDto.getEmail())
                .orElseThrow(() -> new InvalidInputException(ErrorCode.EMAIL_NOT_EXIST));

        if (!passwordEncoder.matches(requestDto.getPassword(), member.getPassword())) {
            throw new InvalidInputException(ErrorCode.WRONG_PASSWORD);
        }

        if (tokenRepository.existsByMemberEmailNot(requestDto.getEmail())) {
            throw new InvalidInputException(ErrorCode.ALREADY_LOGIN);
        }

        // 기존 토큰 삭제
        tokenRepository.deleteByMember(member);

        // JwtMemberDto 생성
        JwtMemberDto jwtMemberDto = new JwtMemberDto(
                member.getId(),
                member.getEmail(),
                member.getRole(),
                LoginType.NORMAL_USER
        );

        // JWT 토큰 생성
        Map<String, String> tokens = jwtProvider.generateTokens(jwtMemberDto);

        // 토큰 저장
        JwtToken jwtToken = JwtToken.builder()
                .accessToken(tokens.get("accessToken"))
                .refreshToken(tokens.get("refreshToken"))
                .member(member)
                .issuedAt(Date.from(Instant.now()))
                .expiredAt(jwtProvider.getExpiration(tokens.get("accessToken")))
                .build();
        tokenRepository.save(jwtToken);

        // 토큰 정보를 포함한 DTO 반환
        return LoginResponseDto.builder()
                .id(member.getId())
                .accessToken(tokens.get("accessToken"))
                .refreshToken(tokens.get("refreshToken"))
                .build();
    }

    @Transactional
    public String refreshAccessToken(String refreshToken) {
        // Refresh Token 검증
        if (!jwtProvider.validateToken(refreshToken)) {
            throw new IllegalArgumentException("Invalid Refresh Token");
        }

        // Refresh Token에서 사용자 이메일 추출
        String email = jwtProvider.getUsername(refreshToken);

        Member member = memberRepository.findByEmail(email)
                .orElseThrow(() -> new InvalidInputException(ErrorCode.EMAIL_NOT_EXIST));

        // 새로운 Access Token 및 Refresh Token 생성
        JwtMemberDto jwtMemberDto = getJwtMemberDto(member);
        Map<String, String> tokens = jwtProvider.generateTokens(jwtMemberDto);

        // 기존 토큰 삭제 및 새로운 토큰 저장
        JwtToken jwtToken = tokenRepository.findByRefreshToken(refreshToken)
                .orElseThrow(() -> new NoAuthorizedException(ErrorCode.JWT_TOKEN_EXPIRED));

        JwtToken updatedJwtToken = JwtToken.builder()
                .member(jwtToken.getMember()) // 기존 Member 유지
                .accessToken(tokens.get("accessToken"))
                .refreshToken(tokens.get("refreshToken"))
                .issuedAt(Date.from(Instant.now()))
                .expiredAt(jwtProvider.getExpiration(tokens.get("accessToken")))
                .build();

        tokenRepository.save(updatedJwtToken);

        return tokens.get("accessToken");
    }

    @Transactional
    // 개발 단계에서만 사용 예정
    public String reissueToken(String email, String password) {
        Member member = memberRepository.findByEmail(email)
                .orElseThrow(() -> new InvalidInputException(ErrorCode.EMAIL_NOT_EXIST));

        //  저절로 인코딩 값이 들어가는데, refresh token이 만료되었을 때, 인코딩된 password가 들어가야 하는지 ??
        if (!member.getPassword().equals(password)) {
            throw new InvalidInputException(ErrorCode.WRONG_PASSWORD);
        }

        JwtMemberDto jwtMemberDto = getJwtMemberDto(member);

        return jwtProvider.generateTokens(jwtMemberDto).get("accessToken");
    }

    private static JwtMemberDto getJwtMemberDto(Member member) {
        JwtMemberDto jwtMemberDto = new JwtMemberDto(
                member.getId(),
                member.getEmail(),
                member.getRole(),
                LoginType.NORMAL_USER
        );
        return jwtMemberDto;
    }

    @Transactional
    public void logout(String accessToken) {
        // Access Token 검증
        if (!jwtProvider.validateToken(accessToken)) {
            throw new NoAuthorizedException(ErrorCode.JWT_TOKEN_EXPIRED);
        }

        // Access Token으로 JWT 삭제
        tokenRepository.deleteByAccessToken(accessToken);
    }
}
