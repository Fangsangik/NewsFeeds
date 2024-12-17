package com.example.newsfeed.comment.dto;

import com.example.newsfeed.comment.entity.Comment;
import com.example.newsfeed.feed.entity.Feed;
import com.example.newsfeed.member.entity.Member;
import lombok.Getter;

@Getter
public class CommentRequestDto {
    private Long parentId;
    private String content;

    public CommentRequestDto(Long parentId, String content) {
        this.parentId = parentId;
        this.content = content;

    }

    public CommentRequestDto(String content) {
        this.content = content;
    }

    public static CommentRequestDto toDto(Comment comment) {
        return new CommentRequestDto(comment.getContent());
    }

    public static Comment toEntity(Member member, Feed feed, CommentRequestDto commentRequestDto, Comment parentComment) {
        return Comment.builder()
                .member(member)
                .feed(feed)
                .content(commentRequestDto.getContent())
                .parent(parentComment)
                .build();
    }

    public boolean isChildComment() {
        return parentId != null;
    }
}
