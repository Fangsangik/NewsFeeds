package com.example.newsfeed.like.dto;

import com.example.newsfeed.like.entity.Like;
import lombok.Getter;

@Getter
public class LikeResponseDto {
    private Long feedId;
    private Integer likeCount;
    private Boolean likedByMe;

    public LikeResponseDto(Long feedId, Integer likeCount, Boolean likedByMe) {
        this.feedId = feedId;
        this.likeCount = likeCount;
        this.likedByMe = likedByMe;
    }

    public static LikeResponseDto of(Long feedId, long count, boolean likedByMe) {
        return new LikeResponseDto(feedId, (int) count, likedByMe);
    }

    // FeedResponseDto의 likes 목록 매핑 호환용 (per-user 행 하나 = 좋아요 1).
    public static LikeResponseDto toDto(Like like) {
        return new LikeResponseDto(like.getFeed().getId(), like.getLikeCount(), null);
    }
}
