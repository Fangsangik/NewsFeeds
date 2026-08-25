package com.example.newsfeed.like.service;

import com.example.newsfeed.like.dto.LikeResponseDto;

public interface LikeService {
    LikeResponseDto getLikeCount(Long feedId, Long memberId);
    LikeResponseDto like(Long feedId, Long memberId);
    LikeResponseDto disLike(Long feedId, Long memberId);
}
