package com.example.newsfeed.kakao.repository;

import com.example.newsfeed.kakao.entity.KakaoMember;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface KakaoMemberRepository extends JpaRepository<KakaoMember, Long> {

    Optional<KakaoMember> findByKakaoId(Long kakaoId);
}
