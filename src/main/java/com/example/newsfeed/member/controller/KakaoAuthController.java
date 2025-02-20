package com.example.newsfeed.member.controller;

import com.example.newsfeed.kakao.service.KakaoService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@Slf4j
@RestController
@RequestMapping("/kakao")
public class KakaoAuthController {
    private final KakaoService kakaoService;

    public KakaoAuthController(KakaoService kakaoService) {
        this.kakaoService = kakaoService;
    }

    /**
     * 카카오 Access Token을 받아 사용자 정보로 우리 JWT 발급
     *
     * @param kakaoAccessToken 클라이언트가 전달한 카카오 Access Token
     * @return 우리 애플리케이션의 JWT
     */
    @PostMapping("/login")
    public ResponseEntity<Map<String, String>> loginWithKakao(@RequestHeader("Authorization") String kakaoAccessToken) {
        // 카카오 사용자 정보 가져오기
        var userInfo = kakaoService.getUserInfo(kakaoAccessToken.replace("Bearer ", ""));

        // 사용자 정보 기반 JWT 생성
        Map<String, String> jwtTokens = kakaoService.createJwtFromKakaoUserInfo(userInfo);

        return ResponseEntity.ok(jwtTokens);
    }

    /**
     * 카카오 로그아웃
     *
     * @param kakaoAccessToken 클라이언트가 전달한 카카오 Access Token
     */
    @PostMapping("/logout")
    public ResponseEntity<Void> logoutFromKakao(@RequestHeader("Authorization") String kakaoAccessToken) {
        kakaoService.logoutFromKakao(kakaoAccessToken.replace("Bearer ", ""));
        return ResponseEntity.noContent().build();
    }
}
