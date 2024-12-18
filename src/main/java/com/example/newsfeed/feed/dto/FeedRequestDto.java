package com.example.newsfeed.feed.dto;

import com.example.newsfeed.feed.entity.Feed;
import com.example.newsfeed.member.entity.Member;
import lombok.Builder;
import lombok.Getter;

@Getter
public class FeedRequestDto {
    private String title;
    private String content;
    private String image;
    private String address;
    private Double latitude;
    private Double longitude;

    @Builder
    public FeedRequestDto(String title, String content, String image, String address, Double latitude, Double longitude) {
        this.title = title;
        this.content = content;
        this.image = image;
        this.address = address;
        this.latitude = latitude;
        this.longitude = longitude;
    }


    public static Feed toDto(Member member, FeedRequestDto feedRequestDto, String address) {
        return Feed.builder()
                .member(member)
                .title(feedRequestDto.getTitle())
                .content(feedRequestDto.getContent())
                .image(feedRequestDto.getImage())
                .address(feedRequestDto.getAddress())
                .latitude(feedRequestDto.getLatitude())
                .longitude(feedRequestDto.getLongitude())
                .address(address)
                .build();
    }
}
