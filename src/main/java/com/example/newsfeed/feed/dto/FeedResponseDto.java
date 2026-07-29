package com.example.newsfeed.feed.dto;

import com.example.newsfeed.comment.dto.CommentResponseDto;
import com.example.newsfeed.feed.entity.Feed;
import com.example.newsfeed.like.dto.LikeResponseDto;
import com.example.newsfeed.member.entity.Member;
import lombok.Builder;
import lombok.Getter;

import java.util.List;
import java.util.stream.Collectors;

@Getter
public class FeedResponseDto {
    private Long feedId;
    private String title;
    private String content;
    private String image;
    private String address;
    private AuthorDto author;
    private List<CommentResponseDto> comments;
    private List<LikeResponseDto> likes;

    @Builder
    public FeedResponseDto(Long feedId, String title, String content, String image, String address,
                           AuthorDto author, List<CommentResponseDto> comments, List<LikeResponseDto> likes) {
        this.feedId = feedId;
        this.title = title;
        this.content = content;
        this.image = image;
        this.address = address;
        this.author = author;
        this.comments = comments;
        this.likes = likes;
    }

    /** 피드 작성자 요약 정보. 프런트가 별도 /members/{feedId}/member 호출 없이 바로 사용. */
    @Getter
    public static class AuthorDto {
        private final Long id;
        private final String name;
        private final String email;
        private final String image;

        public AuthorDto(Long id, String name, String email, String image) {
            this.id = id;
            this.name = name;
            this.email = email;
            this.image = image;
        }

        public static AuthorDto from(Member member) {
            if (member == null) {
                return null;
            }
            return new AuthorDto(member.getId(), member.getName(), member.getEmail(), member.getImage());
        }
    }

    public static FeedResponseDto toDto(Feed feed) {
        List<CommentResponseDto> comments = (feed.getComments() != null) ? feed.getComments().stream()
                .map(CommentResponseDto::toDto)
                .collect(Collectors.toList()) : null;

        List<LikeResponseDto> likes = (feed.getLikes() != null) ? feed.getLikes().stream()
                .map(LikeResponseDto::toDto)
                .collect(Collectors.toList()) : null;

        return FeedResponseDto.builder()
                .feedId(feed.getId())
                .title(feed.getTitle())
                .content(feed.getContent())
                .image(feed.getImage())
                .address(feed.getAddress())
                .author(AuthorDto.from(feed.getMember()))
                .comments(comments)
                .likes(likes)
                .build();
    }
}
