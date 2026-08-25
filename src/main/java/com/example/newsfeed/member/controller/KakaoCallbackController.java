package com.example.newsfeed.member.controller;

import com.example.newsfeed.kakao.service.KakaoService;
import com.example.newsfeed.kakao.dto.KakaoUserInfoResponseDto;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

/**
 * 카카오 인가 코드 콜백. redirect_uri(kakao.redirect_uri = /callback)와 반드시 일치해야 하며,
 * 카카오 개발자 콘솔에도 동일 URI가 등록돼 있어야 한다.
 *
 * 흐름: 카카오 code → access token → 사용자 정보 → 우리 JWT 발급 →
 *       토큰을 localStorage에 심는 최소 부트스트랩 HTML을 반환하고 SPA(/#/)로 이동.
 * (토큰을 URL 쿼리에 노출하지 않도록 서버 렌더 HTML 본문으로 전달한다.)
 */
@Slf4j
@RestController
public class KakaoCallbackController {

    private final KakaoService kakaoService;

    public KakaoCallbackController(KakaoService kakaoService) {
        this.kakaoService = kakaoService;
    }

    @GetMapping("/callback")
    public ResponseEntity<String> callback(@RequestParam("code") String code) {
        String kakaoAccessToken = kakaoService.getAccessToken(code);
        KakaoUserInfoResponseDto userInfo = kakaoService.getUserInfo(kakaoAccessToken);
        Map<String, String> t = kakaoService.createJwtFromKakaoUserInfo(userInfo);

        String html = "<!doctype html><html><head><meta charset=\"utf-8\"><title>로그인</title></head>"
                + "<body><script>"
                + "try{"
                + "localStorage.setItem('nf.accessToken','" + t.get("accessToken") + "');"
                + "localStorage.setItem('nf.refreshToken','" + t.get("refreshToken") + "');"
                + "localStorage.setItem('nf.meId','" + t.get("memberId") + "');"
                + "localStorage.setItem('nf.meEmail','" + t.get("email") + "');"
                + "}catch(e){}"
                + "location.replace('/#/');"
                + "</script>카카오 로그인 처리 중…</body></html>";

        return ResponseEntity.ok().contentType(MediaType.TEXT_HTML).body(html);
    }
}
