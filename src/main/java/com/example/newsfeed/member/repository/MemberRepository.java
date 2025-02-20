package com.example.newsfeed.member.repository;

import com.example.newsfeed.exception.ErrorCode;
import com.example.newsfeed.exception.NotFoundException;
import com.example.newsfeed.kakao.entity.KakaoMember;
import com.example.newsfeed.member.entity.Member;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

import static com.example.newsfeed.exception.ErrorCode.NOT_FOUND_MEMBER;

public interface MemberRepository extends JpaRepository<Member, Long> {
    // PK와 삭제되지 않은 조건으로 멤버 조회
    @Query("SELECT m FROM Member m WHERE m.id = :id AND m.deletedAt IS NULL")
    Optional<Member> findByIdAndNotDeleted(@Param("id") Long id);

    boolean existsByEmail(String email);

    Optional<Member> findByEmail(String email);

    // 피드 아이디 기반 회원 조회
    @Query("SELECT f.member FROM Feed f WHERE f.id = :feedId")
    Optional<Member> findMemberByFeedId(@Param("feedId") Long feedId);

    Optional<Member> findByKakaoMember(KakaoMember kakaoMember);

    default Member findByIdOrElseThrow(Long id) {
        return findByIdAndNotDeleted(id).orElseThrow(() -> new NotFoundException(NOT_FOUND_MEMBER));
    }
}
