package com.example.newsfeed.feed.repository;

import com.example.newsfeed.feed.entity.Feed;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface FeedRepository extends JpaRepository<Feed, Long> {

    // 회원 아이디 기반 피드 조회
    @Query("SELECT f FROM Feed f WHERE f.member.id = :memberId")
    List<Feed> findFeedsByMemberId(@Param("memberId") Long memberId);

    @Query("select f from Feed f Left Join f.likes l group by f.id order by count(l) desc")
    Page<Feed> findAllFeedsOrderByLikeCount(Pageable pageable);
}
