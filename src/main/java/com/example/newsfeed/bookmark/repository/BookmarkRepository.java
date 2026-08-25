package com.example.newsfeed.bookmark.repository;

import com.example.newsfeed.bookmark.entity.Bookmark;
import com.example.newsfeed.feed.dto.FeedWithLikeCountDto;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface BookmarkRepository extends JpaRepository<Bookmark, Long> {

    boolean existsByMember_IdAndFeed_Id(Long memberId, Long feedId);

    @Modifying(clearAutomatically = true)
    @Query("delete from Bookmark b where b.member.id = :memberId and b.feed.id = :feedId")
    int deleteByMemberAndFeed(@Param("memberId") Long memberId, @Param("feedId") Long feedId);

    // 내가 저장한 게시물 목록 (피드 카드 DTO로) — 최신 저장순
    @Query("SELECT new com.example.newsfeed.feed.dto.FeedWithLikeCountDto(f.id, f.title, f.content, f.image, f.member.id, f.member.name, " +
            "(SELECT COUNT(l) FROM com.example.newsfeed.like.entity.Like l WHERE l.feed.id = f.id)) " +
            "FROM Bookmark b JOIN b.feed f WHERE b.member.id = :memberId ORDER BY b.id DESC")
    Page<FeedWithLikeCountDto> findMyBookmarks(@Param("memberId") Long memberId, Pageable pageable);
}
