package com.example.newsfeed.auth.service;

import com.example.newsfeed.auth.dto.LoginRequestDto;
import com.example.newsfeed.auth.jwt.entity.JwtToken;
import com.example.newsfeed.auth.jwt.repository.TokenRepository;
import com.example.newsfeed.auth.jwt.service.JwtProvider;
import com.example.newsfeed.auth.type.LoginType;
import com.example.newsfeed.exception.ErrorCode;
import com.example.newsfeed.exception.InvalidInputException;
import com.example.newsfeed.exception.NoAuthorizedException;
import com.example.newsfeed.member.config.PasswordEncoder;
import com.example.newsfeed.member.entity.Member;
import com.example.newsfeed.member.repository.MemberRepository;
import com.example.newsfeed.member.type.Role;
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


        // 3. 기존 토큰 삭제 (PK를 사용)
        Optional<JwtToken> existingToken = tokenRepository.findByMemberId(member.getId());
        existingToken.ifPresent(tokenRepository::delete);

        Map<String, String> tokens = jwtProvider.generateTokens(member.getId(), Role.USER, LoginType.NORMAL_USER);

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
        Long userId = jwtProvider.getUserIdFromToken(refreshToken);
        return jwtProvider.generateTokens(userId, Role.USER, LoginType.NORMAL_USER).get("accessToken");
    }

    // 개발 단계에서만 사용 예정
    public String reissueToken(String email, String password) {
        Member member = memberRepository.findByEmail(email)
                .orElseThrow(() -> new InvalidInputException(ErrorCode.EMAIL_NOT_EXIST));

        //  저절로 인코딩 값이 들어가는데, refresh token이 만료되었을 때, 인코딩된 password가 들어가야 하는지 ??
        if (!member.getPassword().equals(password)) {
            throw new InvalidInputException(ErrorCode.WRONG_PASSWORD);
        }

        return jwtProvider.generateTokens(member.getId(), Role.USER, LoginType.NORMAL_USER).get("accessToken");
    }

    @Transactional
    public void logout(Long memberId) {
        JwtToken jwtToken = tokenRepository.findByMemberId(memberId)
                .orElseThrow(() -> new NoAuthorizedException(ErrorCode.JWT_TOKEN_EXPIRED));
        tokenRepository.delete(jwtToken);
    }
}
