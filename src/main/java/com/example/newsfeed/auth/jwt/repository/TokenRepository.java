package com.example.newsfeed.auth.jwt.repository;

import com.example.newsfeed.auth.jwt.entity.JwtToken;
import com.example.newsfeed.member.entity.Member;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

public interface TokenRepository extends JpaRepository<JwtToken, Long> {

    boolean existsByMemberEmailNot(String email);

    @Modifying
    @Query("DELETE FROM JwtToken t WHERE t.member = :member")
    void deleteByMember(@Param("member") Member member);


    Optional<JwtToken> findByRefreshToken(String refreshToken);

    @Modifying
    @Query("DELETE FROM JwtToken t WHERE t.accessToken = :accessToken")
    void deleteByAccessToken(@Param("accessToken") String accessToken);

}
