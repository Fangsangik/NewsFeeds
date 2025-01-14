package com.example.newsfeed.feed.service;

import com.example.newsfeed.exception.ErrorCode;
import com.example.newsfeed.exception.NotFoundException;
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

import static com.example.newsfeed.exception.ErrorCode.*;

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
                .orElseThrow(() -> new NotFoundException(NOT_FOUND_MEMBER));

        // 입력값 검증 및 보완
        String address = feedRequestDto.getAddress();
        Double latitude = feedRequestDto.getLatitude();
        Double longitude = feedRequestDto.getLongitude();

        // 주소가 없는 경우, 위도/경도를 통해 주소를 얻음
        if (address == null && latitude != null && longitude != null) {
            address = kakaoGeocodingService.getAddress(latitude, longitude);
        }

        // 좌표가 없는 경우, 주소를 통해 좌표를 얻음
        if ((latitude == null || longitude == null) && address != null) {
            double[] coordinates = kakaoGeocodingService.getCoordinates(address);
            latitude = coordinates[0];
            longitude = coordinates[1];
        }

        // 데이터가 충분하지 않으면 예외 발생
        if (address == null || latitude == null || longitude == null) {
            throw new IllegalArgumentException("주소 또는 좌표 정보가 충분하지 않습니다.");
        }

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
                .orElseThrow(() -> new NotFoundException(NOT_FOUND_NEWSFEED));

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
                .orElseThrow(() -> new NotFoundException(NOT_FOUND_NEWSFEED));

        // Update the feed
        feed.update(feedRequestDto.getTitle(), feedRequestDto.getContent());

        return FeedUpdateResponseDto.toDto(feed);
    }

    @Transactional
    @Override
    public void deleteFeed(Long feedId) {
        // Check if the feed exists
        Feed feed = feedRepository.findById(feedId)
                .orElseThrow(() -> new NotFoundException(NOT_FOUND_NEWSFEED));

        // Delete the feed
        feedRepository.delete(feed);
    }
}
