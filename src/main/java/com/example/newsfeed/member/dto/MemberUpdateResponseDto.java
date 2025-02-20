package com.example.newsfeed.member.dto;

import com.example.newsfeed.member.entity.Member;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Getter
@NoArgsConstructor
public class MemberUpdateResponseDto {
    private String image;
    private String name;
    private String phoneNumber;
    private String address;
    private LocalDateTime updatedAt;

    @Builder
    public MemberUpdateResponseDto(String image, String name, String phoneNumber, String address, LocalDateTime updatedAt) {
        this.image = image;
        this.name = name;
        this.phoneNumber = phoneNumber;
        this.address = address;
        this.updatedAt = updatedAt;
    }

    public static MemberUpdateResponseDto toResponseDto(Member member) {
        return MemberUpdateResponseDto.builder()
                .image(member.getImage())
                .name(member.getName())
                .phoneNumber(member.getPhoneNumber())
                .address(member.getAddress())
                .updatedAt(member.getUpdatedAt())
                .build();
    }
}
