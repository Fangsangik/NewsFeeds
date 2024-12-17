package com.example.newsfeed.feed.dto;

import com.example.newsfeed.feed.entity.Feed;
import com.example.newsfeed.member.entity.Member;
import lombok.Builder;
import lombok.Getter;

@Getter
public class FeedRequestDto {
    private String title;
    private String content;

    @Builder
    public FeedRequestDto(String title, String content) {
        this.title = title;
        this.content = content;
    }


    public static Feed toDto(Member member, FeedRequestDto feedRequestDto) {
        return Feed.builder()
                .member(member)
                .title(feedRequestDto.getTitle())
                .content(feedRequestDto.getContent())
                .build();
    }
}
