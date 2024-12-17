package com.example.newsfeed.comment.controller;

import com.example.newsfeed.comment.dto.CommentChildResponseDto;
import com.example.newsfeed.comment.dto.CommentRequestDto;
import com.example.newsfeed.comment.dto.CommentResponseDto;
import com.example.newsfeed.comment.service.CommentService;
import com.example.newsfeed.constants.response.CommonResponse;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/comments")
public class CommentController {

    private final CommentService commentService;

    public CommentController(CommentService commentService) {
        this.commentService = commentService;
    }

    @PostMapping
    public ResponseEntity<CommonResponse<CommentResponseDto>> createComment
            (@RequestAttribute(name = "memberId") Long memberId,
             @RequestParam Long feedId,
             @RequestBody CommentRequestDto commentRequestDto) {
        CommentResponseDto comment = commentService.createComment(memberId, feedId, commentRequestDto);
        return ResponseEntity.ok(new CommonResponse<>("댓글 작성 완료", comment));
    }

    @PatchMapping("/{commentId}")
    public ResponseEntity<CommonResponse<CommentResponseDto>> fixCommon
            (@RequestAttribute(name = "memberId") Long memberId,
             @PathVariable Long commentId,
             @RequestBody CommentRequestDto commentRequestDto) {
        CommentResponseDto comment = commentService.updateComment(commentId, commentRequestDto);
        return ResponseEntity.ok(new CommonResponse<>("댓글 수정 완료", comment));
    }

    @PostMapping("/{commentId}/child-comments")
    public ResponseEntity<CommonResponse<CommentChildResponseDto>> createChildComment
            (@RequestAttribute(name = "memberId") Long memberId,
             @PathVariable Long commentId,
             @RequestBody CommentRequestDto commentRequestDto) {
        CommentChildResponseDto comment = commentService.createChildComment(commentId, commentRequestDto, memberId);
        return ResponseEntity.ok(new CommonResponse<>("대댓글 작성 완료", comment));
    }

    @GetMapping("/{commentId}")
    public ResponseEntity<CommonResponse<CommentResponseDto>> getComment
            (@PathVariable Long commentId) {
        CommentResponseDto comment = commentService.getComment(commentId);
        return ResponseEntity.ok(new CommonResponse<>("댓글 조회 완료", comment));
    }

    // 특정 피드에 달린 댓글 조회
    @GetMapping("/feed/{feedId}")
    public ResponseEntity<CommonResponse<List<CommentResponseDto>>> getCommentsByFeedId
    (@PathVariable Long feedId) {
        List<CommentResponseDto> comments = commentService.getCommentsByFeedId(feedId);
        return ResponseEntity.ok(new CommonResponse<>("피드 댓글 조회 완료", comments));
    }

    @DeleteMapping("/{commentId}")
    public ResponseEntity<CommonResponse<Void>> deleteComment
            (@RequestAttribute Long memberId,
             @PathVariable Long commentId) {
        commentService.deleteComment(commentId);
        return ResponseEntity.ok(new CommonResponse<>("댓글 삭제 완료"));
    }
}
