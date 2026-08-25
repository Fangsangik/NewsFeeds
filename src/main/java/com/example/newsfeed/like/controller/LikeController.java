package com.example.newsfeed.like.controller;

import com.example.newsfeed.util.AuthenticatedMemberUtil;
import com.example.newsfeed.constants.response.CommonResponse;
import com.example.newsfeed.like.dto.LikeResponseDto;
import com.example.newsfeed.like.service.LikeService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/likes")
public class LikeController {

    private final LikeService likeService;

    public LikeController(LikeService likeService) {
        this.likeService = likeService;
    }

    @PostMapping("/like/{feedId}")
    public ResponseEntity<CommonResponse<LikeResponseDto>> like(@PathVariable Long feedId) {
        Long memberId = AuthenticatedMemberUtil.getAuthenticatedMemberId();
        LikeResponseDto like = likeService.like(feedId, memberId);
        return ResponseEntity.ok(new CommonResponse<>("좋아요 완료", like));
    }

    @PostMapping("/dislike/{feedId}")
    public ResponseEntity<CommonResponse<LikeResponseDto>> disLike(@PathVariable Long feedId) {
        Long memberId = AuthenticatedMemberUtil.getAuthenticatedMemberId();
        LikeResponseDto disLike = likeService.disLike(feedId, memberId);
        return ResponseEntity.ok(new CommonResponse<>("좋아요 취소 완료", disLike));
    }

    // 비로그인도 개수 조회 가능. 로그인 상태면 likedByMe를 함께 반환.
    @GetMapping("/{feedId}")
    public ResponseEntity<CommonResponse<LikeResponseDto>> countByFeedId(@PathVariable Long feedId) {
        Long memberId = AuthenticatedMemberUtil.getAuthenticatedMemberIdOrNull();
        LikeResponseDto count = likeService.getLikeCount(feedId, memberId);
        return ResponseEntity.ok(new CommonResponse<>("좋아요 수 조회", count));
    }
}
