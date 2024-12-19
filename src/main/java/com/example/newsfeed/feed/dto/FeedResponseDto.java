package com.example.newsfeed.feed.dto;

import com.example.newsfeed.comment.dto.CommentResponseDto;
import com.example.newsfeed.comment.entity.Comment;
import com.example.newsfeed.feed.entity.Feed;
import com.example.newsfeed.like.dto.LikeResponseDto;
import com.example.newsfeed.like.entity.Like;
import lombok.Builder;
import lombok.Getter;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@Getter
public class FeedResponseDto {
    private String title;
    private String content;
    private String image;
    private String address;
    private List<CommentResponseDto> comments;
    private List<LikeResponseDto> likes;

    @Builder
    public FeedResponseDto(String title, String content, String image, String address, List<CommentResponseDto> comments, List<LikeResponseDto> likes) {
        this.title = title;
        this.content = content;
        this.image = image;
        this.address = address;
        this.comments = comments;
        this.likes = likes;
    }

    public static FeedResponseDto toDto(Feed feed) {
        List<CommentResponseDto> comments = (feed.getComments() != null) ? feed.getComments().stream()
                .map(CommentResponseDto::toDto)
                .collect(Collectors.toList()) : null;

        List<LikeResponseDto> likes = (feed.getLikes() != null) ? feed.getLikes().stream()
                .map(LikeResponseDto::toDto)
                .collect(Collectors.toList()) : null;

        return FeedResponseDto.builder()
                .title(feed.getTitle())
                .content(feed.getContent())
                .image(feed.getImage())
                .address(feed.getAddress())
                .comments(comments)
                .likes(likes)
                .build();
    }
}
