package com.example.newsfeed.comment.controller;

import com.example.newsfeed.comment.entity.Comment;
import com.example.newsfeed.comment.entity.CommentLike;
import com.example.newsfeed.comment.repository.CommentLikeRepository;
import com.example.newsfeed.comment.repository.CommentRepository;
import com.example.newsfeed.constants.response.CommonResponse;
import com.example.newsfeed.exception.ErrorCode;
import com.example.newsfeed.exception.NotFoundException;
import com.example.newsfeed.member.repository.MemberRepository;
import com.example.newsfeed.util.AuthenticatedMemberUtil;
import org.springframework.http.ResponseEntity;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/comment-likes")
public class CommentLikeController {

    private final CommentLikeRepository commentLikeRepository;
    private final CommentRepository commentRepository;
    private final MemberRepository memberRepository;

    public CommentLikeController(CommentLikeRepository commentLikeRepository, CommentRepository commentRepository,
                                 MemberRepository memberRepository) {
        this.commentLikeRepository = commentLikeRepository;
        this.commentRepository = commentRepository;
        this.memberRepository = memberRepository;
    }

    // 토글 (좋아요 <-> 취소)
    @PostMapping("/{commentId}")
    @Transactional
    public ResponseEntity<CommonResponse<Map<String, Object>>> toggle(@PathVariable Long commentId) {
        Long me = AuthenticatedMemberUtil.getAuthenticatedMemberId();
        boolean liked;
        if (commentLikeRepository.existsByComment_IdAndMember_Id(commentId, me)) {
            commentLikeRepository.deleteByCommentAndMember(commentId, me);
            liked = false;
        } else {
            Comment comment = commentRepository.findById(commentId)
                    .orElseThrow(() -> new NotFoundException(ErrorCode.NOT_FOUND_COMMENT));
            commentLikeRepository.save(CommentLike.builder()
                    .comment(comment)
                    .member(memberRepository.getReferenceById(me))
                    .build());
            liked = true;
        }
        long count = commentLikeRepository.countByComment_Id(commentId);
        return ResponseEntity.ok(new CommonResponse<>("댓글 좋아요 상태 변경",
                Map.of("commentId", commentId, "liked", liked, "count", count)));
    }
}
