package com.example.newsfeed.feed.dto;

import com.example.newsfeed.feed.entity.Feed;
import lombok.Getter;

@Getter
public class FeedWithLikeCountDto {
    private Long feedId;
    private String title;
    private String content;
    private String image;
    private Long authorId;
    private String authorName;
    private long likeCount;

    public FeedWithLikeCountDto(Long feedId, String title, String content, String image,
                               Long authorId, String authorName, long likeCount) {
        this.feedId = feedId;
        this.title = title;
        this.content = content;
        this.image = image;
        this.authorId = authorId;
        this.authorName = authorName;
        this.likeCount = likeCount;
    }

    // JPQL constructor expressions can't handle null SUM aggregates cleanly,
    // so we keep a Long-accepting overload that coerces null to 0.
    public FeedWithLikeCountDto(Long feedId, String title, String content, String image,
                               Long authorId, String authorName, Long likeCount) {
        this(feedId, title, content, image, authorId, authorName, likeCount == null ? 0L : likeCount);
    }

    public static FeedWithLikeCountDto toDto(Feed feed) {
        return new FeedWithLikeCountDto(feed.getId(), feed.getTitle(), feed.getContent(), feed.getImage(),
                feed.getMember() != null ? feed.getMember().getId() : null,
                feed.getMember() != null ? feed.getMember().getName() : null,
                feed.getLikeCount());
    }
}
