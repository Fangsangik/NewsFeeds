package com.example.newsfeed.auth.jwt.repository;

import com.example.newsfeed.auth.jwt.entity.JwtToken;
import com.example.newsfeed.member.entity.Member;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface TokenRepository extends JpaRepository<JwtToken, Long> {

    // 이메일 기반으로 JWT 토큰 조회
    Optional<JwtToken> findByMemberEmail(String email);

    boolean existsByMemberEmailNot(String email);
}
