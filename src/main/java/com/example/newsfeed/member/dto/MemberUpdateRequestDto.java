package com.example.newsfeed.member.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MemberUpdateRequestDto {
    private Long id;
    private String password;        // 현재 비밀번호
    private String name;            // 수정할 이름
    private String phoneNumber;     // 수정할 전화번호
    private String address;         // 수정할 주소
    private String image;           // 수정할 이미지
}
