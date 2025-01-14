package com.example.newsfeed.comment.controller;

import com.example.newsfeed.auth.util.AuthenticatedMemberUtil;
import com.example.newsfeed.comment.dto.CommentChildResponseDto;
import com.example.newsfeed.comment.dto.CommentRequestDto;
import com.example.newsfeed.comment.dto.CommentResponseDto;
import com.example.newsfeed.comment.service.CommentService;
import com.example.newsfeed.constants.response.CommonResponse;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
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
    public ResponseEntity<CommonResponse<CommentResponseDto>> createComment (@RequestBody CommentRequestDto commentRequestDto) {
        Long memberId = AuthenticatedMemberUtil.getAuthenticatedMemberId();

        CommentResponseDto comment = commentService.createComment(commentRequestDto, memberId);
        return ResponseEntity.ok(new CommonResponse<>("댓글 작성 완료", comment));
    }

    @PatchMapping("/{commentId}")
    public ResponseEntity<CommonResponse<CommentResponseDto>> fixComment(@PathVariable Long commentId,
                                                                        @RequestBody CommentRequestDto commentRequestDto) {
        Long memberId = AuthenticatedMemberUtil.getAuthenticatedMemberId();

        CommentResponseDto comment = commentService.updateComment(commentId, commentRequestDto, memberId);
        return ResponseEntity.ok(new CommonResponse<>("댓글 수정 완료", comment));
    }

    @PostMapping("/child-comments")
    public ResponseEntity<CommonResponse<CommentChildResponseDto>> createChildComment (@RequestBody CommentRequestDto commentRequestDto) {
        Long memberId = AuthenticatedMemberUtil.getAuthenticatedMemberId();

        CommentChildResponseDto comment = commentService.createChildComment(commentRequestDto, memberId);
        return ResponseEntity.ok(new CommonResponse<>("대댓글 작성 완료", comment));
    }

    @GetMapping("/{commentId}")
    public ResponseEntity<CommonResponse<CommentResponseDto>> getComment (@PathVariable Long commentId) {
        Long memberId = AuthenticatedMemberUtil.getAuthenticatedMemberId();

        CommentResponseDto comment = commentService.getComment(commentId);
        return ResponseEntity.ok(new CommonResponse<>("댓글 조회 완료", comment));
    }

    // 특정 피드에 달린 댓글 조회
    @GetMapping("/feed/{feedId}")
    public ResponseEntity<CommonResponse<List<CommentResponseDto>>> getCommentsByFeedId (@PathVariable Long feedId) {
        Long memberId = AuthenticatedMemberUtil.getAuthenticatedMemberId();

        List<CommentResponseDto> comments = commentService.getCommentsByFeedId(feedId);
        return ResponseEntity.ok(new CommonResponse<>("피드 댓글 조회 완료", comments));
    }

    @DeleteMapping("/{commentId}")
    public ResponseEntity<CommonResponse<Void>> deleteComment (@PathVariable Long commentId) {
        Long memberId = AuthenticatedMemberUtil.getAuthenticatedMemberId();

        commentService.deleteComment(commentId);
        return ResponseEntity.ok(new CommonResponse<>("댓글 삭제 완료"));
    }
}
