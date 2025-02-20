package com.example.newsfeed.like.service;

import com.example.newsfeed.exception.ErrorCode;
import com.example.newsfeed.exception.NotFoundException;
import com.example.newsfeed.feed.repository.FeedRepository;
import com.example.newsfeed.like.dto.LikeResponseDto;
import com.example.newsfeed.like.entity.Like;
import com.example.newsfeed.like.repository.LikeRepository;
import com.example.newsfeed.member.entity.Member;
import com.example.newsfeed.member.repository.MemberRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class LikeServiceImpl implements LikeService {

    private final LikeRepository likeRepository;
    private final FeedRepository feedRepository;
    private final MemberRepository memberRepository;

    public LikeServiceImpl(LikeRepository likeRepository, FeedRepository feedRepository, MemberRepository memberRepository) {
        this.likeRepository = likeRepository;
        this.feedRepository = feedRepository;
        this.memberRepository = memberRepository;
    }

    @Transactional
    @Override
    public LikeResponseDto like(Member member, Long feedId) {
       if (!memberRepository.existsById(member.getId())) {
            throw new NotFoundException(ErrorCode.NOT_FOUND_MEMBER);
       }

        Like like = likeRepository.findById(feedId)
                .orElseGet(() -> {
                    Like newLike = Like.builder()
                            .feed(feedRepository.findByIdOrElseThrow(feedId))
                            .likeCount(0)
                            .build();
                    likeRepository.save(newLike); // 새 Like는 저장
                    return newLike;
                });

        like.upCount();
        likeRepository.save(like);

        return LikeResponseDto.toDto(like);
    }


    @Transactional
    @Override
    public LikeResponseDto disLike(Member member, Long feedId) {
        if (!memberRepository.existsById(member.getId())) {
            throw new NotFoundException(ErrorCode.NOT_FOUND_MEMBER);
        }

        List<Like> likes = likeRepository.findByFeedId(feedId);

        if (likes.size() > 1) {
            // 중복 데이터를 제거하고 하나만 유지
            likeRepository.deleteAll(likes.subList(1, likes.size()));
        }

        if (likes.isEmpty()) {
            throw new NotFoundException(ErrorCode.NOT_FOUND_LIKE);
        }

        Like like = likes.get(0);
        like.downCount();
        likeRepository.save(like);

        return LikeResponseDto.toDto(like);
    }


    @Transactional(readOnly = true)
    @Override
    public LikeResponseDto getLikeCount(Long feedId) {
        feedRepository.findById(feedId)
                .orElseThrow(() -> new NotFoundException(ErrorCode.NOT_FOUND_NEWSFEED));

        Long likeCount = likeRepository.countByFeedId(feedId);// DB에서 좋아요 개수 조회

        return LikeResponseDto.fromCount(feedId, likeCount); // DTO로 변환
    }
}
