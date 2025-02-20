package com.example.newsfeed.like.service;


import com.example.newsfeed.like.dto.LikeResponseDto;
import com.example.newsfeed.member.entity.Member;

public interface LikeService {
    LikeResponseDto getLikeCount(Long feedId);
    LikeResponseDto like(Member member, Long feedId);
    LikeResponseDto disLike(Member member, Long feedId);
}
