package com.example.newsfeed.feed.dto;

import com.example.newsfeed.feed.entity.Feed;
import lombok.Getter;

@Getter
public class FeedWithLikeCountDto {
    private Long feedId;
    private String title;
    private String content;
    private String image;
    private long likeCount;

    public FeedWithLikeCountDto(Long feedId, String title, String content, String image, long likeCount) {
        this.feedId = feedId;
        this.title = title;
        this.content = content;
        this.image = image;
        this.likeCount = likeCount;
    }

    // JPQL constructor expressions can't handle null SUM aggregates cleanly,
    // so we keep a Long-accepting overload that coerces null to 0.
    public FeedWithLikeCountDto(Long feedId, String title, String content, String image, Long likeCount) {
        this(feedId, title, content, image, likeCount == null ? 0L : likeCount);
    }

    public static FeedWithLikeCountDto toDto(Feed feed) {
        return new FeedWithLikeCountDto(feed.getId(), feed.getTitle(), feed.getContent(), feed.getImage(), feed.getLikeCount());
    }
}
