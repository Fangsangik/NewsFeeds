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

    public void updatedMember(MemberUpdateRequestDto updatedDto) {
        if (updatedDto.getName() != null) {
            this.name = updatedDto.getName();
        }

        if (updatedDto.getImage() != null) {
            this.image = updatedDto.getImage();
        }

        if (updatedDto.getPhoneNumber() != null) {
            this.phoneNumber = updatedDto.getPhoneNumber();
        }

        if (updatedDto.getAddress() != null) {
            this.address = updatedDto.getAddress();
        }
    }

    public void updatePassword(String password) {
        this.password = password;
    }
}
