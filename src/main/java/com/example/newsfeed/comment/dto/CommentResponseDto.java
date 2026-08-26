package com.example.newsfeed.comment.dto;

import com.example.newsfeed.comment.entity.Comment;
import lombok.Builder;
import lombok.Getter;

import java.util.ArrayList;
import java.util.List;

@Getter
@lombok.Setter
@Builder
public class CommentResponseDto {
    private Long commentId; // 댓글 ID
    private String content; // 댓글 내용
    private Long feedId; // 피드 ID
    private Long authorId; // 작성자 ID
    private String authorName; // 작성자 이름
    private Long likeCount; // 댓글 좋아요 수
    private Boolean likedByMe; // 내가 이 댓글을 좋아요 했는지
    private List<CommentResponseDto> childComments; // 대댓글 리스트

    public static CommentResponseDto toDto(Comment comment) {
        return CommentResponseDto.builder()
                .commentId(comment.getId())
                .content(comment.getContent())
                .feedId(comment.getFeed().getId())
                .authorId(comment.getMember() != null ? comment.getMember().getId() : null)
                .authorName(comment.getMember() != null ? comment.getMember().getName() : null)
                .childComments(comment.getChildren() != null ?
                        comment.getChildren().stream()
                                .map(CommentResponseDto::toDto)
                                .toList() : new ArrayList<>()) // Null 방지 및 대댓글 리스트 변환
                .build();
    }
}
