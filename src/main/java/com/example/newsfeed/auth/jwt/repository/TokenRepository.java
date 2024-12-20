package com.example.newsfeed.auth.jwt.repository;

import com.example.newsfeed.auth.jwt.entity.JwtToken;
import com.example.newsfeed.member.entity.Member;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface TokenRepository extends JpaRepository<JwtToken, Long> {

    Optional<JwtToken> findByMemberId(Long memberId);

    boolean existsByMemberEmailNot(String email);
}
