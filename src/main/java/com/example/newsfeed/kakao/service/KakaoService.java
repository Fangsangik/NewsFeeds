package com.example.newsfeed.kakao.service;

import com.example.newsfeed.auth.jwt.JwtProvider;
import com.example.newsfeed.auth.type.LoginType;
import com.example.newsfeed.kakao.dto.KakaoTokenResponseDto;
import com.example.newsfeed.kakao.dto.KakaoUserInfoResponseDto;
import com.example.newsfeed.member.service.MemberService;
import com.example.newsfeed.member.type.Role;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;

import java.util.Map;


@Slf4j
@Service
public class KakaoService {

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

        Long memberId = memberService.findOrCreateMember(kakaoId, email, nickname);
        return jwtProvider.generateTokens(memberId, Role.USER, LoginType.KAKAO_USER);
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

