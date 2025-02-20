package com.example.newsfeed.member.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
public class PasswordRequestDto {
    @NotBlank(message = "현재 비밀번호는 필수 값 입니다.")
    private String oldPassword;
    @NotBlank(message = "새로운 비밀번호는 필수 값 입니다.")
    private String newPassword;

    public PasswordRequestDto(String oldPassword, String newPassword) {
        this.oldPassword = oldPassword;
        this.newPassword = newPassword;
    }
}
