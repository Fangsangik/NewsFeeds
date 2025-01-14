package com.example.newsfeed.auth.jwt.service;

import com.example.newsfeed.auth.jwt.dto.JwtMemberDto;
import com.example.newsfeed.member.type.Role;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import javax.crypto.SecretKey;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

@Slf4j
@Service
public class JwtProvider {

    // 비밀키 JWT 서명을 위해 사용 HS256 알고리즘 사용
    private final SecretKey secretKey;

    public JwtProvider(@Value("${spring.jwt.secret}") String secret) {
        this.secretKey = new SecretKeySpec(secret.getBytes(StandardCharsets.UTF_8), "HmacSHA256");
    }

    // 토큰 생성
    public Map<String, String> generateTokens(JwtMemberDto member) {
        Map<String, String> tokens = new HashMap<>();

        // Access Token 생성
        Date now = new Date();
        Date accessTokenExpireDate = new Date(now.getTime() + 1000L * 60 * 60); // 1시간 유효기간
        Map<String, Object> claims = new HashMap<>();
        claims.put("email", member.getEmail());
        claims.put("memberId", member.getId());
        claims.put("role", member.getRole().name());
        claims.put("loginType", member.getLoginType().name());

        String accessToken = Jwts.builder()
                .setClaims(claims)
                .setSubject(member.getEmail())
                .setIssuedAt(now)
                .setExpiration(accessTokenExpireDate)
                .signWith(secretKey, SignatureAlgorithm.HS256)
                .compact();

        // Refresh Token 생성
        Date refreshTokenExpireDate = new Date(now.getTime() + 1000L * 60 * 60 * 24 * 7); // 7일 유효기간
        String refreshToken = Jwts.builder()
                .setSubject(member.getEmail())
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

    public String getUsername(String token) {
        Claims claims = this.getClaims(token);
        log.info("Claims during token generation: {}", claims);
        log.info("Extracted username from token: {}", claims.getSubject());
        return claims.getSubject();

    }

    public Long getMemberId(String token) {
        Claims claims = this.getClaims(token);
        return claims.get("memberId", Long.class);
    }

    private Claims getClaims(String token) {
        return Jwts.parserBuilder()
                .setSigningKey(secretKey)
                .build()
                .parseClaimsJws(token)
                .getBody();
    }
}
