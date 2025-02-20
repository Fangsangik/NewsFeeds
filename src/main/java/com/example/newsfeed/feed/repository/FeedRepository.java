package com.example.newsfeed.feed.repository;

import com.example.newsfeed.exception.ErrorCode;
import com.example.newsfeed.exception.NotFoundException;
import com.example.newsfeed.feed.dto.FeedWithLikeCountDto;
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

    @Query("SELECT new com.example.newsfeed.feed.dto.FeedWithLikeCountDto(f.id, f.title, f.content, SUM(l.likeCount)) " +
            "FROM Feed f LEFT JOIN f.likes l GROUP BY f.id ORDER BY SUM(l.likeCount) DESC")
    Page<FeedWithLikeCountDto> findAllFeedsOrderByLikeCount(Pageable pageable);

    default Feed findByIdOrElseThrow(Long id) {
        return findById(id).orElseThrow(() -> new NotFoundException(ErrorCode.NOT_FOUND_NEWSFEED));
    }
}
