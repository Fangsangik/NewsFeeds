package com.example.newsfeed.comment.service;

import com.example.newsfeed.comment.dto.CommentChildResponseDto;
import com.example.newsfeed.comment.dto.CommentRequestDto;
import com.example.newsfeed.comment.dto.CommentResponseDto;

import java.util.List;

public interface CommentService {

    CommentResponseDto createComment(Long memberId, Long feedId, CommentRequestDto commentRequestDto);
    CommentResponseDto updateComment(Long commentId, CommentRequestDto commentRequestDto);
    CommentResponseDto getComment(Long commentId);
    CommentChildResponseDto createChildComment(Long parentId, CommentRequestDto requestDto, Long memberId);
    void deleteComment(Long commentId);
    List<CommentResponseDto> getCommentsByFeedId(Long feedId);
}
