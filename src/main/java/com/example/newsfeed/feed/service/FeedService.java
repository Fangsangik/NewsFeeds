package com.example.newsfeed.feed.service;

import com.example.newsfeed.feed.dto.FeedRequestDto;
import com.example.newsfeed.feed.dto.FeedResponseDto;
import com.example.newsfeed.feed.dto.FeedUpdateResponseDto;
import com.example.newsfeed.feed.dto.FeedWithLikeCountDto;
import org.springframework.data.domain.Page;

import java.util.List;

public interface FeedService {
    FeedResponseDto createFeed(Long memberId, FeedRequestDto feedRequestDto);
    FeedUpdateResponseDto updateFeed(Long feedId, FeedRequestDto feedRequestDto);
    void deleteFeed(Long id);
    FeedResponseDto getFeed(Long feedId);
    List<FeedResponseDto> getFeedsByMemberId(Long memberId);
    Page<FeedWithLikeCountDto> getAllFeedsOrderByLikeCount(int page, int size);
}
