package com.example.newsfeed.comment.dto;

import com.example.newsfeed.comment.entity.Comment;
import lombok.Getter;

@Getter
public class CommentChildResponseDto {
    private Long commentId; // 댓글 ID
    private String content; // 댓글 내용
    private Long feedId; // 피드 ID

    public CommentChildResponseDto(Long commentId, String content, Long feedId) {
        this.commentId = commentId;
        this.content = content;
        this.feedId = feedId;
    }

    public static CommentChildResponseDto toDto(Comment comment) {
        return new CommentChildResponseDto(comment.getId(), comment.getContent(), comment.getFeed().getId());
    }
}
