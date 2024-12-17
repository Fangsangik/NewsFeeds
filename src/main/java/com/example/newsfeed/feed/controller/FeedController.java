package com.example.newsfeed.feed.controller;

import com.example.newsfeed.constants.response.CommonResponse;
import com.example.newsfeed.feed.dto.FeedRequestDto;
import com.example.newsfeed.feed.dto.FeedResponseDto;
import com.example.newsfeed.feed.dto.FeedUpdateResponseDto;
import com.example.newsfeed.feed.dto.FeedWithLikeCountDto;
import com.example.newsfeed.feed.service.FeedService;
import org.springframework.data.domain.Page;
import org.springframework.http.ResponseEntity;
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
    public ResponseEntity<CommonResponse<FeedResponseDto>> createFeed
            (@RequestAttribute(name = "memberId") Long memberId,
             @RequestBody FeedRequestDto feedRequestDto) {
        FeedResponseDto feed = feedService.createFeed(memberId, feedRequestDto);
        System.out.println("Received MemberId: " + memberId);
        return ResponseEntity.ok(new CommonResponse<>("피드 작성 완료", feed));
    }


    @GetMapping("/{feedId}")
    public ResponseEntity<CommonResponse<FeedResponseDto>> getFeed
            (
             @PathVariable Long feedId) {
        FeedResponseDto feed = feedService.getFeed(feedId);
        return ResponseEntity.ok(new CommonResponse<>("피드 조회 완료", feed));
    }

    // 위치기 변경 되어야 하는거 아닌가 라는 의심
    // 회원 기반 피드 조회
    @GetMapping("/member/{memberId}")
    public ResponseEntity<CommonResponse<List<FeedResponseDto>>> getFeedsByMemberId(@PathVariable Long memberId) {
        List<FeedResponseDto> feeds = feedService.getFeedsByMemberId(memberId);
        return ResponseEntity.ok(new CommonResponse<>("회원 기반 피드 조회 완료", feeds));
    }

    @GetMapping("/likecount")
    public ResponseEntity<CommonResponse<Page<FeedWithLikeCountDto>>> getAllCounts
            (@RequestParam (defaultValue = "0") int page,
             @RequestParam(defaultValue = "10") int size) {
        Page<FeedWithLikeCountDto> feeds = feedService.getAllFeedsOrderByLikeCount(page, size);
        return ResponseEntity.ok(new CommonResponse<>("좋아요 수 기반 피드 조회 완료", feeds));

    }
    @PatchMapping("/{feedId}")
    public ResponseEntity<CommonResponse<FeedUpdateResponseDto>> updateFeed
            (@RequestAttribute(name = "memberId") Long memberId,
             @PathVariable Long feedId,
             @RequestBody FeedRequestDto feedRequestDto) {
        FeedUpdateResponseDto updateFeed = feedService.updateFeed(feedId, feedRequestDto);
        return ResponseEntity.ok(new CommonResponse<>("피드 수정 완료", updateFeed));
    }

    @DeleteMapping("/{feedId}")
    public ResponseEntity<CommonResponse<Void>> deleteFeed
            (@RequestAttribute(name = "memberId") Long memberId,
             @PathVariable Long feedId) {
        feedService.deleteFeed(feedId);
        return ResponseEntity.ok(new CommonResponse<>("피드 삭제 완료"));
    }
}
