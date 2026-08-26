package com.example.newsfeed.comment.entity;

import com.example.newsfeed.member.entity.Member;
import jakarta.persistence.*;
import lombok.Builder;
import lombok.Getter;

import static jakarta.persistence.FetchType.LAZY;
import static jakarta.persistence.GenerationType.IDENTITY;

/** 댓글 좋아요 (per-user: 회원당 댓글 1개에 최대 1행). */
@Entity
@Getter
@Table(name = "comment_likes",
        uniqueConstraints = @UniqueConstraint(columnNames = {"member_id", "comment_id"}))
public class CommentLike {

    @Id
    @GeneratedValue(strategy = IDENTITY)
    private Long id;

    @ManyToOne(fetch = LAZY)
    @JoinColumn(name = "member_id")
    private Member member;

    @ManyToOne(fetch = LAZY)
    @JoinColumn(name = "comment_id")
    private Comment comment;

    public CommentLike() {}

    @Builder
    public CommentLike(Member member, Comment comment) {
        this.member = member;
        this.comment = comment;
    }
}
