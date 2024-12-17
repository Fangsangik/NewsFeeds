package com.example.newsfeed.member.dto;

import com.example.newsfeed.member.entity.Member;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MemberUpdateResponseDto {
    private String image;
    private String name;
    private String phoneNumber;
    private String address;
    private LocalDateTime updatedAt;


    public static MemberUpdateResponseDto toResponseDto(Member member) {
        return MemberUpdateResponseDto.builder()
                .name(member.getName())
                .phoneNumber(member.getPhoneNumber())
                .address(member.getAddress())
                .image(member.getImage())
                .updatedAt(member.getUpdatedAt())
                .build();
    }
}
