package com.example.newsfeed.feed.dto;

import com.example.newsfeed.feed.entity.Feed;
import lombok.Builder;
import lombok.Getter;

@Getter
public class FeedResponseDto {
    private String title;

    @Builder
    public FeedResponseDto(String title) {
        this.title = title;
    }

    public static FeedResponseDto toDto(Feed feed) {
        return FeedResponseDto.builder()
                .title(feed.getTitle())
                .build();
    }
}
