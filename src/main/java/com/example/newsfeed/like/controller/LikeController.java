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
    private final com.example.newsfeed.like.repository.LikeRepository likeRepository;
    private final com.example.newsfeed.member.repository.MemberRepository memberRepository;

    public LikeController(LikeService likeService,
                          com.example.newsfeed.like.repository.LikeRepository likeRepository,
                          com.example.newsfeed.member.repository.MemberRepository memberRepository) {
        this.likeService = likeService;
        this.likeRepository = likeRepository;
        this.memberRepository = memberRepository;
    }

    // 특정 피드를 좋아요한 사람 목록 (프로필 이동용)
    @GetMapping("/{feedId}/members")
    public ResponseEntity<CommonResponse<java.util.List<com.example.newsfeed.friend.dto.FriendMemberDto>>> likers(@PathVariable Long feedId) {
        java.util.List<Long> ids = likeRepository.findLikerMemberIds(feedId);
        java.util.Map<Long, com.example.newsfeed.member.entity.Member> byId = memberRepository.findAllById(ids).stream()
                .collect(java.util.stream.Collectors.toMap(com.example.newsfeed.member.entity.Member::getId, m -> m));
        java.util.List<com.example.newsfeed.friend.dto.FriendMemberDto> out = ids.stream()
                .map(byId::get).filter(java.util.Objects::nonNull)
                .map(com.example.newsfeed.friend.dto.FriendMemberDto::new).toList();
        return ResponseEntity.ok(new CommonResponse<>("좋아요한 사람", out));
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
