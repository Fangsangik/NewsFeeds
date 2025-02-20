package com.example.newsfeed.comment.service;

import com.example.newsfeed.comment.dto.CommentChildResponseDto;
import com.example.newsfeed.comment.dto.CommentRequestDto;
import com.example.newsfeed.comment.dto.CommentResponseDto;
import com.example.newsfeed.member.entity.Member;

import java.util.List;

public interface CommentService {

    CommentResponseDto createComment(CommentRequestDto commentRequestDto, Member member);
    CommentResponseDto updateComment(Long commentId, CommentRequestDto commentRequestDto, Member member);
    CommentResponseDto getComment(Long commentId);
    CommentChildResponseDto createChildComment(CommentRequestDto requestDto, Member member);
    void deleteComment(Member member, Long commentId);
    List<CommentResponseDto> getCommentsByFeedId(Long feedId);
}
