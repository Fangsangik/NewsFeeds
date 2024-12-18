package com.example.newsfeed.feed.dto;

import com.example.newsfeed.comment.entity.Comment;
import com.example.newsfeed.feed.entity.Feed;
import com.example.newsfeed.like.entity.Like;
import lombok.Builder;
import lombok.Getter;

import java.util.List;

@Getter
public class FeedResponseDto {
    private String title;
    private String content;
    private String image;
    private String address;
    private List<Comment> comments;
    private List<Like> likes;

    @Builder
    public FeedResponseDto(String title, String content, String image, String address, List<Comment> comments, List<Like> likes) {
        this.title = title;
        this.content = content;
        this.image = image;
        this.address = address;
        this.comments = comments;
        this.likes = likes;
    }

    public static FeedResponseDto toDto(Feed feed) {
        return FeedResponseDto.builder()
                .title(feed.getTitle())
                .content(feed.getContent())
                .image(feed.getImage())
                .address(feed.getAddress())
                .comments(feed.getComments())
                .likes(feed.getLikes())
                .build();
    }
}
