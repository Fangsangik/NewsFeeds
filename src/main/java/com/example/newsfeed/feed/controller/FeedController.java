package com.example.newsfeed.feed.controller;

import com.example.newsfeed.auth.jwt.service.UserDetailsImpl;
import com.example.newsfeed.util.AuthenticatedMemberUtil;
import com.example.newsfeed.constants.response.CommonResponse;
import com.example.newsfeed.feed.dto.FeedRequestDto;
import com.example.newsfeed.feed.dto.FeedResponseDto;
import com.example.newsfeed.feed.dto.FeedUpdateResponseDto;
import com.example.newsfeed.feed.dto.FeedWithLikeCountDto;
import com.example.newsfeed.feed.service.FeedService;
import com.example.newsfeed.member.entity.Member;
import org.springframework.data.domain.Page;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/feeds")
public class FeedController {

    private final FeedService feedService;

    public FeedController(FeedService feedService) {
        this.feedService = feedService;
    }

    @PostMapping
    public ResponseEntity<CommonResponse<FeedResponseDto>> createFeed (@AuthenticationPrincipal UserDetailsImpl userDetails,
                                                                       @RequestBody FeedRequestDto feedRequestDto) {
        Member member = AuthenticatedMemberUtil.getMember(userDetails);
        FeedResponseDto feed = feedService.createFeed(member, feedRequestDto);
        System.out.println("Received MemberId: " + member.getId());
        return ResponseEntity.ok(new CommonResponse<>("피드 작성 완료", feed));
    }


    @GetMapping("/{feedId}")
    public ResponseEntity<CommonResponse<FeedResponseDto>> getFeed (@PathVariable Long feedId) {
        FeedResponseDto feed = feedService.getFeed(feedId);
        return ResponseEntity.ok(new CommonResponse<>("피드 조회 완료", feed));
    }

    // 위치기 변경 되어야 하는거 아닌가 라는 의심
    // 회원 기반 피드 조회
    @GetMapping("/members/{memberId}")
    public ResponseEntity<CommonResponse<List<FeedResponseDto>>> getFeedsByMemberId(Long memberId) {

        List<FeedResponseDto> feeds = feedService.getFeedsByMemberId(memberId);
        return ResponseEntity.ok(new CommonResponse<>("회원 기반 피드 조회 완료", feeds));
    }

    @GetMapping("/likecount")
    public ResponseEntity<CommonResponse<Page<FeedWithLikeCountDto>>> getAllCounts(@RequestParam (defaultValue = "0") int page,
                                                                                   @RequestParam(defaultValue = "10") int size) {
        Page<FeedWithLikeCountDto> feeds = feedService.getAllFeedsOrderByLikeCount(page, size);
        return ResponseEntity.ok(new CommonResponse<>("좋아요 수 기반 피드 조회 완료", feeds));

    }
    @PatchMapping("/{feedId}")
    public ResponseEntity<CommonResponse<FeedUpdateResponseDto>> updateFeed(@AuthenticationPrincipal UserDetailsImpl userDetails,
                                                                            @PathVariable Long feedId,
                                                                            @RequestBody FeedRequestDto feedRequestDto) {
        Member member = AuthenticatedMemberUtil.getMember(userDetails);
        FeedUpdateResponseDto updateFeed = feedService.updateFeed(member, feedId, feedRequestDto);
        return ResponseEntity.ok(new CommonResponse<>("피드 수정 완료", updateFeed));
    }

    @DeleteMapping("/{feedId}")
    public ResponseEntity<CommonResponse<Void>> deleteFeed(@AuthenticationPrincipal UserDetailsImpl userDetails,
                                                           @PathVariable Long feedId) {

        Member member = AuthenticatedMemberUtil.getMember(userDetails);
        feedService.deleteFeed(member, feedId);
        return ResponseEntity.ok(new CommonResponse<>("피드 삭제 완료"));
    }
}
