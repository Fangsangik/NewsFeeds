package com.example.newsfeed.feed.dto;

import com.example.newsfeed.feed.entity.Feed;
import com.example.newsfeed.member.entity.Member;
import lombok.Builder;
import lombok.Getter;

import java.util.ArrayList;
import java.util.List;

@Getter
public class FeedRequestDto {
    private final String title;
    private final String content;
    private final String image;          // 단일 이미지(하위호환)
    private final List<String> images;   // 다중 이미지(신규)
    private final String address;
    private final Double latitude;
    private final Double longitude;

    @Builder
    public FeedRequestDto(String title, String content, String image, List<String> images, String address, Double latitude, Double longitude) {
        this.title = title;
        this.content = content;
        this.image = image;
        this.images = images;
        this.address = address;
        this.latitude = latitude;
        this.longitude = longitude;
    }


    public static Feed toDto(Member member, FeedRequestDto feedRequestDto, String address, Double latitude, Double longitude) {
        // images 우선, 없으면 단일 image를 1장짜리 목록으로. 커버(image)는 목록 첫 장.
        List<String> imgs = new ArrayList<>();
        if (feedRequestDto.getImages() != null) imgs.addAll(feedRequestDto.getImages());
        if (imgs.isEmpty() && feedRequestDto.getImage() != null && !feedRequestDto.getImage().isBlank()) {
            imgs.add(feedRequestDto.getImage());
        }
        String cover = imgs.isEmpty() ? null : imgs.get(0);

        return Feed.builder()
                .member(member)
                .title(feedRequestDto.getTitle())
                .content(feedRequestDto.getContent())
                .image(cover)
                .images(imgs)
                .address(address)
                .latitude(latitude)
                .longitude(longitude)
                .build();
    }
}
