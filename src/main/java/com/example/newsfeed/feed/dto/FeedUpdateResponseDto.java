package com.example.newsfeed.feed.dto;

import com.example.newsfeed.feed.entity.Feed;
import lombok.Getter;

@Getter
public class FeedUpdateResponseDto {
    
    private String title;
    private String content;

    public FeedUpdateResponseDto(String title, String content) {
        this.title = title;
        this.content = content;
    }

    public static FeedUpdateResponseDto toDto(Feed feed) {
        return new FeedUpdateResponseDto(feed.getTitle(), feed.getContent());
    }
}
