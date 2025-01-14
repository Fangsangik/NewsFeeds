package com.example.newsfeed.like.service;


import com.example.newsfeed.like.dto.LikeResponseDto;

public interface LikeService {
    LikeResponseDto getLikeCount(Long feedId);
    LikeResponseDto like(Long feedId);
    LikeResponseDto disLike(Long feedId);
}
