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

    @Query("SELECT new com.example.newsfeed.feed.dto.FeedWithLikeCountDto(f.id, f.title, f.content, f.image, f.member.id, f.member.name, COUNT(l)) " +
            "FROM Feed f LEFT JOIN f.likes l GROUP BY f.id, f.title, f.content, f.image, f.member.id, f.member.name ORDER BY COUNT(l) DESC")
    Page<FeedWithLikeCountDto> findAllFeedsOrderByLikeCount(Pageable pageable);

    // 최신순 피드 (뉴스피드 기본 정렬)
    @Query("SELECT new com.example.newsfeed.feed.dto.FeedWithLikeCountDto(f.id, f.title, f.content, f.image, f.member.id, f.member.name, COUNT(l)) " +
            "FROM Feed f LEFT JOIN f.likes l GROUP BY f.id, f.title, f.content, f.image, f.member.id, f.member.name ORDER BY f.id DESC")
    Page<FeedWithLikeCountDto> findAllFeedsOrderByLatest(Pageable pageable);

    // 팔로우(친구) 기반 피드: 내 글 + 수락된 친구들의 글, 최신순
    @Query("SELECT new com.example.newsfeed.feed.dto.FeedWithLikeCountDto(f.id, f.title, f.content, f.image, f.member.id, f.member.name, COUNT(l)) " +
            "FROM Feed f LEFT JOIN f.likes l " +
            "WHERE f.member.id = :me OR f.member.id IN (" +
            "  SELECT CASE WHEN fr.sender.id = :me THEN fr.receiver.id ELSE fr.sender.id END FROM Friend fr " +
            "  WHERE (fr.sender.id = :me OR fr.receiver.id = :me) AND fr.status = com.example.newsfeed.friend.type.FriendRequestStatus.ACCEPTED) " +
            "GROUP BY f.id, f.title, f.content, f.image, f.member.id, f.member.name ORDER BY f.id DESC")
    Page<FeedWithLikeCountDto> findFollowingFeed(@Param("me") Long me, Pageable pageable);

    // 게시물 검색 (제목/내용 부분일치, 해시태그 포함) — 최신순
    @Query("SELECT new com.example.newsfeed.feed.dto.FeedWithLikeCountDto(f.id, f.title, f.content, f.image, f.member.id, f.member.name, COUNT(l)) " +
            "FROM Feed f LEFT JOIN f.likes l " +
            "WHERE LOWER(f.title) LIKE LOWER(CONCAT('%', :q, '%')) OR LOWER(f.content) LIKE LOWER(CONCAT('%', :q, '%')) " +
            "GROUP BY f.id, f.title, f.content, f.image, f.member.id, f.member.name ORDER BY f.id DESC")
    Page<FeedWithLikeCountDto> searchFeeds(@Param("q") String q, Pageable pageable);

    default Feed findByIdOrElseThrow(Long id) {
        return findById(id).orElseThrow(() -> new NotFoundException(ErrorCode.NOT_FOUND_NEWSFEED));
    }
}
