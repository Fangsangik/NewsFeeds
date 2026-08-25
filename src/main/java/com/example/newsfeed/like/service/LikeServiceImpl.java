package com.example.newsfeed.like.service;

import com.example.newsfeed.exception.ErrorCode;
import com.example.newsfeed.exception.NotFoundException;
import com.example.newsfeed.feed.entity.Feed;
import com.example.newsfeed.feed.repository.FeedRepository;
import com.example.newsfeed.like.dto.LikeResponseDto;
import com.example.newsfeed.like.entity.Like;
import com.example.newsfeed.like.repository.LikeRepository;
import com.example.newsfeed.member.repository.MemberRepository;
import com.example.newsfeed.notification.entity.Notification;
import com.example.newsfeed.notification.service.NotificationService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * per-user 좋아요: 회원당 최대 1행. 좋아요 = 행 생성(중복이면 무시),
 * 취소 = 행 삭제, 개수 = 행 수, 내가 눌렀는지 = 존재 여부.
 */
@Service
public class LikeServiceImpl implements LikeService {

    private final LikeRepository likeRepository;
    private final FeedRepository feedRepository;
    private final MemberRepository memberRepository;
    private final NotificationService notificationService;

    public LikeServiceImpl(LikeRepository likeRepository, FeedRepository feedRepository,
                           MemberRepository memberRepository, NotificationService notificationService) {
        this.likeRepository = likeRepository;
        this.feedRepository = feedRepository;
        this.memberRepository = memberRepository;
        this.notificationService = notificationService;
    }

    @Transactional
    @Override
    public LikeResponseDto like(Long feedId, Long memberId) {
        Feed feed = feedRepository.findById(feedId)
                .orElseThrow(() -> new NotFoundException(ErrorCode.NOT_FOUND_NEWSFEED));

        if (!likeRepository.existsByFeed_IdAndMember_Id(feedId, memberId)) {
            likeRepository.save(Like.builder()
                    .feed(feed)
                    .member(memberRepository.getReferenceById(memberId))
                    .likeCount(1)
                    .build());
            // 게시물 작성자에게 좋아요 알림 (본인 글 좋아요는 service에서 무시)
            String actorName = memberRepository.findById(memberId).map(m -> m.getName()).orElse("사용자");
            notificationService.notify(feed.getMember().getId(), Notification.Type.LIKE, memberId, actorName,
                    feedId, actorName + "님이 회원님의 게시물을 좋아합니다.");
        }
        return LikeResponseDto.of(feedId, likeRepository.countByFeedId(feedId), true);
    }

    @Transactional
    @Override
    public LikeResponseDto disLike(Long feedId, Long memberId) {
        likeRepository.deleteByFeedAndMember(feedId, memberId);
        return LikeResponseDto.of(feedId, likeRepository.countByFeedId(feedId), false);
    }

    @Transactional(readOnly = true)
    @Override
    public LikeResponseDto getLikeCount(Long feedId, Long memberId) {
        feedRepository.findById(feedId)
                .orElseThrow(() -> new NotFoundException(ErrorCode.NOT_FOUND_NEWSFEED));

        boolean likedByMe = memberId != null && likeRepository.existsByFeed_IdAndMember_Id(feedId, memberId);
        return LikeResponseDto.of(feedId, likeRepository.countByFeedId(feedId), likedByMe);
    }
}
