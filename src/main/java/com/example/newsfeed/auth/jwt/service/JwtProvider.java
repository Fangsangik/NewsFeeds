package com.example.newsfeed.auth.jwt.service;

import com.example.newsfeed.auth.type.LoginType;
import com.example.newsfeed.member.type.Role;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import javax.crypto.SecretKey;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

@Service
public class JwtProvider {

    // 비밀키 JWT 서명을 위해 사용 HS256 알고리즘 사용
    private final SecretKey secretKey;

    public JwtProvider(@Value("${spring.jwt.secret}") String secret) {
        this.secretKey = new SecretKeySpec(secret.getBytes(StandardCharsets.UTF_8), "HmacSHA256");
    }

    // 토큰 생성
    public Map<String, String> generateTokens(Long memberId, Role role, LoginType loginType) {
        Map<String, String> tokens = new HashMap<>();

        // Access Token 생성
        Date now = new Date();
        Date accessTokenExpireDate = new Date(now.getTime() + 1000L * 60 * 60); // 1시간 유효기간
        Map<String, Object> claims = new HashMap<>();
        claims.put("memberId", memberId);
        claims.put("role", role.name());
        claims.put("loginType", loginType.name());

        String accessToken = Jwts.builder()
                .setClaims(claims)
                .setSubject(String.valueOf(memberId))
                .setIssuedAt(now)
                .setExpiration(accessTokenExpireDate)
                .signWith(secretKey, SignatureAlgorithm.HS256)
                .compact();

        // Refresh Token 생성
        Date refreshTokenExpireDate = new Date(now.getTime() + 1000L * 60 * 60 * 24 * 7); // 7일 유효기간
        String refreshToken = Jwts.builder()
                .setSubject(String.valueOf(memberId))
                .setIssuedAt(now)
                .setExpiration(refreshTokenExpireDate)
                .signWith(secretKey, SignatureAlgorithm.HS256)
                .compact();

        // 생성된 토큰을 반환
        tokens.put("accessToken", accessToken);
        tokens.put("refreshToken", refreshToken);
        return tokens;
    }

    // 토큰 검증
    public boolean validateToken(String token) {
        try {
            if (token == null || token.isEmpty()) {
                return false;
            }
            Jwts.parserBuilder()
                    .setSigningKey(secretKey)
                    .build()
                    .parseClaimsJws(token);
            return true;
        } catch (Exception e) {
            return false;
        }
    }

    public Long getUserIdFromToken(String token) {
        return Jwts.parserBuilder()
                .setSigningKey(secretKey) // 비밀 키로 서명 검증
                .build()
                .parseClaimsJws(token) // 토큰을 파싱하고 유효성 검증
                .getBody() // 토큰의 body 반환
                .get("memberId", Long.class);
    }

    public Role getRoleFromToken(String token) {
        return Role.valueOf(Jwts.parserBuilder()
                .setSigningKey(secretKey) // 비밀 키로 서명 검증
                .build()
                .parseClaimsJws(token) // 토큰을 파싱하고 유효성 검증
                .getBody() // 토큰의 body 반환
                .get("role", String.class));
    }

}
