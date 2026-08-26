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
    public FeedResponseDto createFeed(Member member, FeedRequestDto feedRequestDto) {
        // Check if the member exists
        Member findMember = memberRepository.findByIdOrElseThrow(member.getId());

        // 입력값 검증 및 보완
        String address = feedRequestDto.getAddress();
        Double latitude = feedRequestDto.getLatitude();
        Double longitude = feedRequestDto.getLongitude();

        boolean hasAddress = address != null && !address.isBlank();
        boolean hasCoords = latitude != null && longitude != null;

        // 좌표만 있으면 주소를, 주소만 있으면 좌표를 보완한다.
        // 둘 다 없으면 위치 없이 게시(위치는 선택 항목).
        if (hasCoords && !hasAddress) {
            address = kakaoGeocodingService.getAddress(latitude, longitude);
        } else if (!hasCoords && hasAddress) {
            double[] coordinates = kakaoGeocodingService.getCoordinates(address);
            latitude = coordinates[0];
            longitude = coordinates[1];
        }

        // Create a feed (보정된 주소/좌표를 저장)
        Feed feed = FeedRequestDto.toDto(findMember, feedRequestDto, address, latitude, longitude);

        // Save the feed
        return FeedResponseDto.toDto(feedRepository.save(feed));
    }

    @Override
    public FeedResponseDto getFeed(Long feedId) {
        // Check if the feed exists
        Feed feed = feedRepository.findById(feedId)
                .orElseThrow(() -> new NotFoundException(NOT_FOUND_NEWSFEED));

        return FeedResponseDto.toDto(feed);
    }

    // 회원 아이디 기반 피드 조회
    @Override
    public List<FeedResponseDto> getFeedsByMemberId(Long memberId) {
        // Feed 엔터티 리스트를 조회
        List<Feed> feeds = feedRepository.findFeedsByMemberId(memberId);

        // Feed 엔터티를 FeedResponseDto로 변환하여 반환
        return feeds.stream()
                .map(FeedResponseDto::toDto)
                .collect(Collectors.toList());
    }

    @Override
    public Page<FeedWithLikeCountDto> getAllFeedsOrderByLikeCount(int page, int size) {
        return feedRepository.findAllFeedsOrderByLikeCount(PageRequest.of(page, size));
    }

    @Override
    public Page<FeedWithLikeCountDto> getAllFeedsOrderByLatest(int page, int size) {
        return feedRepository.findAllFeedsOrderByLatest(PageRequest.of(page, size));
    }

    @Override
    public Page<FeedWithLikeCountDto> getFollowingFeed(Long memberId, int page, int size) {
        return feedRepository.findFollowingFeed(memberId, PageRequest.of(page, size));
    }

    @Override
    public Page<FeedWithLikeCountDto> searchFeeds(String q, int page, int size) {
        return feedRepository.searchFeeds(q == null ? "" : q.trim(), PageRequest.of(page, size));
    }

    @Transactional
    @Override
    public FeedUpdateResponseDto updateFeed(Member member, Long feedId, FeedRequestDto feedRequestDto) {
        if (!memberRepository.existsById(member.getId())) {
            throw new NotFoundException(ErrorCode.NOT_FOUND_MEMBER);
        }

        Feed feed = feedRepository.findByIdOrElseThrow(feedId);
        feed.update(feedRequestDto.getTitle(), feedRequestDto.getContent());
        // 이미지 편집: images가 전달되면 목록 교체(빈 배열이면 이미지 제거). null이면 그대로 유지.
        feed.replaceImages(feedRequestDto.getImages());

        return FeedUpdateResponseDto.toDto(feed);
    }

    @Transactional
    @Override
    public void deleteFeed(Member member, Long feedId) {
        if (!memberRepository.existsById(member.getId())) {
            throw new NotFoundException(ErrorCode.NOT_FOUND_MEMBER);
        }

        Feed feed = feedRepository.findByIdOrElseThrow(feedId);

        // Delete the feed
        feedRepository.delete(feed);
    }
}
