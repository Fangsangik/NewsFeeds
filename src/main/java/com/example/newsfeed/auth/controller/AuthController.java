package com.example.newsfeed.auth.controller;

import com.example.newsfeed.auth.dto.AuthLoginDto;
import com.example.newsfeed.auth.dto.LoginRequestDto;
import com.example.newsfeed.auth.service.AuthService;
import com.example.newsfeed.constants.response.CommonResponse;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

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
        AuthLoginDto response = authService.login(requestDto);
        return ResponseEntity.ok(new CommonResponse<>("로그인 성공", response));
    }

    @PostMapping("/refresh")
    public ResponseEntity<Map<String, String>> refreshAccessToken(@RequestBody Map<String, String> request) {
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
}
