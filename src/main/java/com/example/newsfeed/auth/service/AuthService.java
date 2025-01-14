package com.example.newsfeed.auth.service;

import com.example.newsfeed.auth.dto.LoginRequestDto;
import com.example.newsfeed.auth.jwt.dto.JwtMemberDto;
import com.example.newsfeed.auth.jwt.entity.JwtToken;
import com.example.newsfeed.auth.jwt.repository.TokenRepository;
import com.example.newsfeed.auth.jwt.service.JwtProvider;
import com.example.newsfeed.auth.type.LoginType;
import com.example.newsfeed.exception.ErrorCode;
import com.example.newsfeed.exception.InvalidInputException;
import com.example.newsfeed.exception.NoAuthorizedException;
import com.example.newsfeed.member.entity.Member;
import com.example.newsfeed.member.repository.MemberRepository;
import com.example.newsfeed.member.type.Role;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Date;
import java.util.Map;
import java.util.Optional;

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
    public void login(LoginRequestDto requestDto) {

        Member member = memberRepository.findByEmail(requestDto.getEmail())
                .orElseThrow(() -> new InvalidInputException(ErrorCode.EMAIL_NOT_EXIST));

        if (!passwordEncoder.matches(requestDto.getPassword(), member.getPassword())) {
            throw new InvalidInputException(ErrorCode.WRONG_PASSWORD);
        }

        if (tokenRepository.existsByMemberEmailNot(requestDto.getEmail())) {
            throw new InvalidInputException(ErrorCode.ALREADY_LOGIN);
        }

        // 기존 토큰 삭제
        Optional<JwtToken> existingToken = tokenRepository.findByMemberEmail(member.getEmail());
        existingToken.ifPresent(tokenRepository::delete);

        // JwtMemberDto 생성
        JwtMemberDto jwtMemberDto = getJwtMemberDto(member);

        Map<String, String> tokens = jwtProvider.generateTokens(jwtMemberDto);

        JwtToken newJwtToken = JwtToken.builder()
                .member(member)
                .accessToken(tokens.get("accessToken"))
                .refreshToken(tokens.get("refreshToken"))
                .expiredAt(new Date(System.currentTimeMillis() + 1000L * 60 * 60 * 24 * 7))
                .issuedAt(new Date(System.currentTimeMillis()))
                .build();

        tokenRepository.save(newJwtToken);
    }


    public String refreshAccessToken(String refreshToken) {
        if (!jwtProvider.validateToken(refreshToken)) {
            throw new IllegalArgumentException("Invalid Refresh Token");
        }
        String email = jwtProvider.getUsername(refreshToken);

        Member member = memberRepository.findByEmail(email)
                .orElseThrow(() -> new InvalidInputException(ErrorCode.EMAIL_NOT_EXIST));


        JwtMemberDto jwtMemberDto = getJwtMemberDto(member);

        return jwtProvider.generateTokens(jwtMemberDto).get("accessToken");
    }

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
        if (!jwtProvider.validateToken(accessToken)) {
            throw new NoAuthorizedException(ErrorCode.JWT_TOKEN_EXPIRED);
        }

        // 토큰에서 이메일 또는 사용자 ID 추출
        String email = jwtProvider.getUsername(accessToken);

        // JWT 토큰 삭제
        JwtToken jwtToken = tokenRepository.findByMemberEmail(email)
                .orElseThrow(() -> new NoAuthorizedException(ErrorCode.JWT_TOKEN_EXPIRED));
        tokenRepository.delete(jwtToken);
    }
}
