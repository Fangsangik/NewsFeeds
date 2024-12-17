package com.example.newsfeed.feed.dto;

import com.example.newsfeed.feed.entity.Feed;
import lombok.Getter;

@Getter
public class FeedWithLikeCountDto {
    private Long feedId;
    private String title;
    private Integer likeCount;

    public FeedWithLikeCountDto(Long feedId, String title, Integer likeCount) {
        this.feedId = feedId;
        this.title = title;
        this.likeCount = likeCount;
    }

    public static FeedWithLikeCountDto toDto(Feed feed) {
        return new FeedWithLikeCountDto(feed.getId(), feed.getTitle(), feed.getLikeCount());
    }
}
