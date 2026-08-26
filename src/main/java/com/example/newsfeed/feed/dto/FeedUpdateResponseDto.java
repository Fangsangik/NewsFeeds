package com.example.newsfeed.feed.dto;

import com.example.newsfeed.feed.entity.Feed;
import lombok.Getter;

@Getter
public class FeedUpdateResponseDto {

    private String title;
    private String content;
    private String image;
    private java.util.List<String> images;

    public FeedUpdateResponseDto(String title, String content, String image, java.util.List<String> images) {
        this.title = title;
        this.content = content;
        this.image = image;
        this.images = images;
    }

    public static FeedUpdateResponseDto toDto(Feed feed) {
        return new FeedUpdateResponseDto(feed.getTitle(), feed.getContent(), feed.getImage(), feed.getImages());
    }
}
