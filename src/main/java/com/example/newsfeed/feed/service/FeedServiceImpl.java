package com.example.newsfeed.feed.service;

import com.example.newsfeed.feed.dto.FeedRequestDto;
import com.example.newsfeed.feed.dto.FeedResponseDto;
import com.example.newsfeed.feed.dto.FeedUpdateResponseDto;
import com.example.newsfeed.feed.dto.FeedWithLikeCountDto;
import com.example.newsfeed.feed.entity.Feed;
import com.example.newsfeed.feed.repository.FeedRepository;
import com.example.newsfeed.kakao.service.KakaoGeocodingService;
import com.example.newsfeed.member.entity.Member;
import com.example.newsfeed.member.repository.MemberRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
public class FeedServiceImpl implements FeedService {

    private final FeedRepository feedRepository;
    private final MemberRepository memberRepository;
    private final KakaoGeocodingService kakaoGeocodingService;

    public FeedServiceImpl(FeedRepository feedRepository, MemberRepository memberRepository, KakaoGeocodingService kakaoGeocodingService) {
        this.feedRepository = feedRepository;
        this.memberRepository = memberRepository;
        this.kakaoGeocodingService = kakaoGeocodingService;
    }

    @Transactional
    @Override
    public FeedResponseDto createFeed(Long memberId, FeedRequestDto feedRequestDto) {
        // Check if the member exists
        Member member = memberRepository.findById(memberId)
                .orElseThrow(() -> new IllegalArgumentException("Member does not exist"));

        // Get the address from the latitude and longitude
        String address = kakaoGeocodingService.getAddress(feedRequestDto.getLatitude(), feedRequestDto.getLongitude());

        // Create a feed
        Feed feed = FeedRequestDto.toDto(member, feedRequestDto, address);


        // Save the feed
        return FeedResponseDto.toDto(feedRepository.save(feed));
    }

    @Transactional(readOnly = true)
    @Override
    public FeedResponseDto getFeed(Long feedId) {
        // Check if the feed exists
        Feed feed = feedRepository.findById(feedId)
                .orElseThrow(() -> new IllegalArgumentException("Feed does not exist"));

        return FeedResponseDto.toDto(feed);
    }

    @Transactional(readOnly = true)
    // 회원 아이디 기반 피드 조회
    public List<FeedResponseDto> getFeedsByMemberId(Long memberId) {
        // Feed 엔터티 리스트를 조회
        List<Feed> feeds = feedRepository.findFeedsByMemberId(memberId);

        // Feed 엔터티를 FeedResponseDto로 변환하여 반환
        return feeds.stream()
                .map(FeedResponseDto::toDto)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    @Override
    public Page<FeedWithLikeCountDto> getAllFeedsOrderByLikeCount(int page, int size) {
        return feedRepository.findAllFeedsOrderByLikeCount(PageRequest.of(page, size));
    }

    @Transactional
    @Override
    public FeedUpdateResponseDto updateFeed(Long feedId, FeedRequestDto feedRequestDto) {
        // Check if the feed exists
        Feed feed = feedRepository.findById(feedId)
                .orElseThrow(() -> new IllegalArgumentException("Feed does not exist"));

        // Update the feed
        feed.update(feedRequestDto.getTitle(), feedRequestDto.getContent());

        return FeedUpdateResponseDto.toDto(feed);
    }

    @Transactional
    @Override
    public void deleteFeed(Long feedId) {
        // Check if the feed exists
        Feed feed = feedRepository.findById(feedId)
                .orElseThrow(() -> new IllegalArgumentException("Feed does not exist"));

        // Delete the feed
        feedRepository.delete(feed);
    }
}
