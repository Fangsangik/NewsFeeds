package com.example.newsfeed.like.dto;

import com.example.newsfeed.like.entity.Like;
import lombok.Getter;

@Getter
public class LikeResponseDto {
    private Long feedId;
    private Integer likeCount;

    public LikeResponseDto(Long feedId, Integer likeCount) {
        this.feedId = feedId;
        this.likeCount = likeCount;
    }

    // 새로운 팩토리 메서드
    public static LikeResponseDto fromCount(Long feedId, Long count) {
        return new LikeResponseDto(feedId, count.intValue());
    }

    // 기존 DTO 변환 메서드 (엔터티에서 변환)
    public static LikeResponseDto toDto(Like like) {
        return new LikeResponseDto(like.getFeed().getId(), like.getLikeCount());
    }
}