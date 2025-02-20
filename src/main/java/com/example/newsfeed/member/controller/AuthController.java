package com.example.newsfeed.member.controller;

import com.example.newsfeed.member.dto.LoginRequestDto;
import com.example.newsfeed.member.dto.LoginResponseDto;
import com.example.newsfeed.member.service.AuthService;
import com.example.newsfeed.constants.response.CommonResponse;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/auth")
public class AuthController {

    private final AuthService authService;

    public AuthController(AuthService authService) {
        this.authService = authService;
    }

    @PostMapping("/login")
    public ResponseEntity<CommonResponse<LoginResponseDto>> login(@RequestBody LoginRequestDto requestDto) {
        LoginResponseDto login = authService.login(requestDto);
        return ResponseEntity.ok(new CommonResponse<>("로그인 성공", login));
    }

    // 기존 토큰 갱신 (refresh token)
    @PostMapping("/refresh")
    public ResponseEntity<Map<String, String>> refreshAccessToken (@RequestBody Map<String, String> request) {
        String refreshToken = request.get("refreshToken");
        String newAccessToken = authService.refreshAccessToken(refreshToken);

        return ResponseEntity.ok(Map.of("accessToken", newAccessToken));
    }

    @PostMapping("/logout")
    public ResponseEntity<CommonResponse<String>> logout(@RequestHeader("Authorization") String authorizationHeader) {
        // Authorization 헤더에서 토큰 추출
        String accessToken = authorizationHeader.replace("Bearer ", "");

        // 로그아웃 처리
        authService.logout(accessToken);

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
