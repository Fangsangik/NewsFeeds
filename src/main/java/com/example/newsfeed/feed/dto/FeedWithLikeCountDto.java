package com.example.newsfeed.feed.dto;

import com.example.newsfeed.feed.entity.Feed;
import lombok.Getter;

@Getter
public class FeedWithLikeCountDto {
    private Long feedId;
    private String title;
    private String content;
    private long likeCount;

    public FeedWithLikeCountDto(Long feedId, String title, String content, long likeCount) {
        this.feedId = feedId;
        this.title = title;
        this.content = content;
        this.likeCount = likeCount;
    }
    public static FeedWithLikeCountDto toDto(Feed feed) {
        return new FeedWithLikeCountDto(feed.getId(), feed.getTitle(), feed.getContent(), feed.getLikeCount());
    }
}
