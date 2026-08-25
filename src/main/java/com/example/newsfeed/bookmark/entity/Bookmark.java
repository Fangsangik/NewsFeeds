package com.example.newsfeed.bookmark.entity;

import com.example.newsfeed.feed.entity.Feed;
import com.example.newsfeed.member.entity.Member;
import jakarta.persistence.*;
import lombok.Builder;
import lombok.Getter;

import static jakarta.persistence.FetchType.LAZY;
import static jakarta.persistence.GenerationType.IDENTITY;

/** 게시물 저장(북마크). 회원당 피드 1개에 최대 1행. */
@Entity
@Getter
@Table(name = "bookmarks",
        uniqueConstraints = @UniqueConstraint(columnNames = {"member_id", "feed_id"}))
public class Bookmark {

    @Id
    @GeneratedValue(strategy = IDENTITY)
    private Long id;

    @ManyToOne(fetch = LAZY)
    @JoinColumn(name = "member_id")
    private Member member;

    @ManyToOne(fetch = LAZY)
    @JoinColumn(name = "feed_id")
    private Feed feed;

    public Bookmark() {}

    @Builder
    public Bookmark(Member member, Feed feed) {
        this.member = member;
        this.feed = feed;
    }
}
