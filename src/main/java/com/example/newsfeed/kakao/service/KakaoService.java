package com.example.newsfeed.kakao.service;

import com.example.newsfeed.auth.jwt.dto.JwtMemberDto;
import com.example.newsfeed.auth.jwt.service.JwtProvider;
import com.example.newsfeed.member.type.LoginType;
import com.example.newsfeed.kakao.dto.KakaoTokenResponseDto;
import com.example.newsfeed.kakao.dto.KakaoUserInfoResponseDto;
import com.example.newsfeed.member.service.MemberService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;


@Slf4j
@Service
public class KakaoService {

    /** 카카오 인가 코드 요청용 authorize URL (프런트 '카카오 로그인' 버튼이 여기로 리다이렉트). */
    public String getAuthorizeUrl() {
        return "https://kauth.kakao.com/oauth/authorize?response_type=code"
                + "&client_id=" + clientId
                + "&redirect_uri=" + URLEncoder.encode(redirectUri, StandardCharsets.UTF_8);
    }

    private final JwtProvider jwtProvider;
    private final MemberService memberService;

    @Value("${kakao.client_id}")
    private String clientId;

    @Value("${kakao.client_secret}")
    private String clientSecret;

    @Value("${kakao.redirect_uri}")
    private String redirectUri;

    @Value("${kakao.token_uri}")
    private String tokenUri;

    @Value("${kakao.user_info_uri}")
    private String userInfoUri;

    public KakaoService(JwtProvider jwtProvider, MemberService memberService) {
        this.jwtProvider = jwtProvider;
        this.memberService = memberService;
    }

    public String getAccessToken(String code) {
        KakaoTokenResponseDto response = WebClient.create()
                .post()
                .uri(tokenUri)
                .headers(headers -> headers.setContentType(MediaType.APPLICATION_FORM_URLENCODED))
                .bodyValue("grant_type=authorization_code"
                        + "&client_id=" + clientId
                        + "&redirect_uri=" + redirectUri
                        + "&code=" + code
                        + "&client_secret=" + clientSecret)
                .retrieve()
                .bodyToMono(KakaoTokenResponseDto.class)
                .block();

        return response.getAccessToken();
    }

    public KakaoUserInfoResponseDto getUserInfo(String accessToken) {
        return WebClient.create()
                .get()
                .uri(userInfoUri)
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + accessToken)
                .retrieve()
                .bodyToMono(KakaoUserInfoResponseDto.class)
                .block();
    }

    public Map<String, String> createJwtFromKakaoUserInfo(KakaoUserInfoResponseDto userInfo) {
        Long kakaoId = userInfo.getId();
        String email = userInfo.getKakaoAccount().getEmail();
        String nickname = userInfo.getKakaoAccount().getProfile().getNickname();

        JwtMemberDto member = memberService.findOrCreateMember(kakaoId, email, nickname);

        // JwtMemberDto 생성
        JwtMemberDto jwtMemberDto = new JwtMemberDto(
                member.getId(),
                member.getEmail(),
                member.getRole(),
                LoginType.KAKAO_USER
        );

        // 콜백 부트스트랩이 프런트 localStorage(meId/meEmail)까지 채울 수 있도록 함께 반환.
        Map<String, String> tokens = new HashMap<>(jwtProvider.generateTokens(jwtMemberDto));
        tokens.put("memberId", String.valueOf(member.getId()));
        tokens.put("email", member.getEmail());
        return tokens;
    }

    /**
     * 카카오 로그아웃 (Access Token 사용)
     */
    public void logoutFromKakao(String kakaoAccessToken) {
        log.info("Logging out from Kakao with access token: {}", kakaoAccessToken);

        try {
            WebClient.create()
                    .post()
                    .uri("https://kapi.kakao.com/v1/user/logout")
                    .header(HttpHeaders.AUTHORIZATION, "Bearer " + kakaoAccessToken)
                    .retrieve()
                    .toBodilessEntity()
                    .block();

            log.info("Kakao logout successful.");
        } catch (Exception e) {
            log.error("Kakao logout failed: {}", e.getMessage());
            throw new RuntimeException("Kakao 로그아웃 실패", e);
        }
    }
}

