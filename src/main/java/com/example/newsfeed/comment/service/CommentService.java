package com.example.newsfeed.comment.service;

import com.example.newsfeed.comment.dto.CommentChildResponseDto;
import com.example.newsfeed.comment.dto.CommentRequestDto;
import com.example.newsfeed.comment.dto.CommentResponseDto;

import java.util.List;

public interface CommentService {

    CommentResponseDto createComment(CommentRequestDto commentRequestDto, Long authenticatedMemberId);
    CommentResponseDto updateComment(Long commentId, CommentRequestDto commentRequestDto, Long authenticatedMemberId);
    CommentResponseDto getComment(Long commentId);
    CommentChildResponseDto createChildComment(CommentRequestDto requestDto, Long authenticatedMemberId);
    void deleteComment(Long commentId);
    List<CommentResponseDto> getCommentsByFeedId(Long feedId);
}
