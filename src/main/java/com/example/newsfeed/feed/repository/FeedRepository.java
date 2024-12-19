package com.example.newsfeed.feed.repository;

import com.example.newsfeed.comment.entity.Comment;
import com.example.newsfeed.feed.dto.FeedWithLikeCountDto;
import com.example.newsfeed.feed.entity.Feed;
import com.example.newsfeed.like.entity.Like;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface FeedRepository extends JpaRepository<Feed, Long> {

    // 회원 아이디 기반 피드 조회
    @Query("SELECT f FROM Feed f WHERE f.member.id = :memberId")
    List<Feed> findFeedsByMemberId(@Param("memberId") Long memberId);

    @Query("SELECT new com.example.newsfeed.feed.dto.FeedWithLikeCountDto(f.id, f.title, f.content, SUM(l.likeCount)) " +
            "FROM Feed f LEFT JOIN f.likes l GROUP BY f.id ORDER BY SUM(l.likeCount) DESC")
    Page<FeedWithLikeCountDto> findAllFeedsOrderByLikeCount(Pageable pageable);
}
