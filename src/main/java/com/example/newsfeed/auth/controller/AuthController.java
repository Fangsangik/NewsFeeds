package com.example.newsfeed.auth.controller;

import com.example.newsfeed.auth.dto.AuthLoginDto;
import com.example.newsfeed.auth.dto.LoginRequestDto;
import com.example.newsfeed.auth.service.AuthService;
import com.example.newsfeed.constants.response.CommonResponse;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/oauth")
public class AuthController {

    private final AuthService authService;

    public AuthController(AuthService authService) {
        this.authService = authService;
    }

    @PostMapping("/login")
    public ResponseEntity<CommonResponse<AuthLoginDto>> login(@RequestBody LoginRequestDto requestDto) {
        authService.login(requestDto);
        return ResponseEntity.ok(new CommonResponse<>("로그인 성공"));
    }

    // 기존 토큰 갱신 (refresh token)
    @PostMapping("/refresh")
    public ResponseEntity<Map<String, String>> refreshAccessToken(
            @RequestAttribute("memberId") Long memberId,
            @RequestBody Map<String, String> request) {
        String refreshToken = request.get("refreshToken");
        String newAccessToken = authService.refreshAccessToken(refreshToken);

        return ResponseEntity.ok(Map.of("accessToken", newAccessToken));
    }

    @PostMapping("/logout")
    public ResponseEntity<CommonResponse<String>> logout(HttpServletRequest request) {
        HttpSession session = request.getSession();
        session.invalidate();
        return ResponseEntity.ok(new CommonResponse<>("로그아웃 성공"));
    }

    // refresh token이 만료되었을 때, access token 재발급
    @PostMapping("/reissue")
    public ResponseEntity<String> reissueToken(
            @RequestParam String email,
            @RequestParam String password) {
        String newAccessToken = authService.reissueToken(email, password);
        return ResponseEntity.ok(newAccessToken);
    }
}
