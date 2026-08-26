package com.example.newsfeed.like.repository;

import com.example.newsfeed.like.entity.Like;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface LikeRepository extends JpaRepository<Like, Long> {

    // per-user 모델: 좋아요 수 = 좋아요 행 개수(회원당 1행).
    @Query("select count(l) from Like l where l.feed.id = :feedId")
    long countByFeedId(@Param("feedId") Long feedId);

    boolean existsByFeed_IdAndMember_Id(Long feedId, Long memberId);

    @Modifying(clearAutomatically = true)
    @Query("delete from Like l where l.feed.id = :feedId and l.member.id = :memberId")
    int deleteByFeedAndMember(@Param("feedId") Long feedId, @Param("memberId") Long memberId);

    // 특정 피드를 좋아요한 회원 id 목록 (최근 순)
    @Query("select l.member.id from Like l where l.feed.id = :feedId order by l.id desc")
    java.util.List<Long> findLikerMemberIds(@Param("feedId") Long feedId);
}
