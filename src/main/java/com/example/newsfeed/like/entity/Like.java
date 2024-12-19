package com.example.newsfeed.like.entity;

import com.example.newsfeed.feed.entity.Feed;
import com.example.newsfeed.member.entity.Member;
import jakarta.persistence.*;
import lombok.Builder;
import lombok.Getter;

import static jakarta.persistence.GenerationType.IDENTITY;

/**
 * 좋아요를 나타내는 Entity
 * 한개의 피트에 여러개의 좋아요가 달릴 수 있다.
 * user와 
 */
@Entity
@Getter
@Table(name = "likes")
public class Like {
    @Id
    @GeneratedValue(strategy = IDENTITY)
    private Long id;

    private Integer likeCount;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "feed_id")
    private Feed feed;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "member_id")
    private Member member;

    public Like() {
    }

    @Builder
    public Like(Long id, Feed feed, Member member, Integer likeCount) {
        this.id = id;
        this.feed = feed;
        this.member = member;
        this.likeCount = likeCount;
    }

    public void upCount(){
        this.likeCount++;
    }

    public void downCount(){
        if (this.likeCount > 0) {
            this.likeCount--;
        } else {
            throw new IllegalArgumentException("좋아요 개수는 0보다 작을 수 없습니다.");
        }
    }
}
