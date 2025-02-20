package com.example.newsfeed.like.controller;

import com.example.newsfeed.auth.jwt.service.UserDetailsImpl;
import com.example.newsfeed.util.AuthenticatedMemberUtil;
import com.example.newsfeed.constants.response.CommonResponse;
import com.example.newsfeed.like.dto.LikeResponseDto;
import com.example.newsfeed.like.service.LikeService;
import com.example.newsfeed.like.service.LikeServiceImpl;
import com.example.newsfeed.member.entity.Member;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/likes")
public class LikeController {

    private final LikeService likeService;

    public LikeController(LikeServiceImpl likeService) {
        this.likeService = likeService;
    }

    @PostMapping("/like/{feedId}")
    public ResponseEntity<CommonResponse<LikeResponseDto>> like (@PathVariable Long feedId,
                                                                 @AuthenticationPrincipal UserDetailsImpl userDetails) {

        Member member = AuthenticatedMemberUtil.getMember(userDetails);
        LikeResponseDto like = likeService.like(member, feedId);
        return ResponseEntity.ok(new CommonResponse<>("좋아요 완료", like));
    }

    @PostMapping("/dislike/{feedId}")
    public ResponseEntity<CommonResponse<LikeResponseDto>> disLike (@PathVariable Long feedId,
                                                                    @AuthenticationPrincipal UserDetailsImpl userDetails) {
        Member member = AuthenticatedMemberUtil.getMember(userDetails);
        LikeResponseDto disLike = likeService.disLike(member, feedId);
        return ResponseEntity.ok(new CommonResponse<>("좋아요 취소 완료", disLike));
    }

    @GetMapping("/{feedId}")
    public ResponseEntity<CommonResponse<LikeResponseDto>> countByFeedId(@PathVariable Long feedId) {
        LikeResponseDto count = likeService.getLikeCount(feedId);
        return ResponseEntity.ok(new CommonResponse<>("좋아요 수 조회", count));
    }

}
