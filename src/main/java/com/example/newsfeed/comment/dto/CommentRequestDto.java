package com.example.newsfeed.comment.dto;

import com.example.newsfeed.comment.entity.Comment;
import com.example.newsfeed.feed.entity.Feed;
import com.example.newsfeed.member.entity.Member;
import lombok.Getter;

@Getter
public class CommentRequestDto {
    private final Long memberId;
    private final Long feedId;
    private final Long parentId;
    private final String content;

    public CommentRequestDto(Long memberId, Long feedId, Long parentId, String content) {
        this.memberId = memberId;
        this.feedId = feedId;
        this.parentId = parentId;
        this.content = content;
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
