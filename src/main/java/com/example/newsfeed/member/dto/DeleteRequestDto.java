package com.example.newsfeed.member.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
@AllArgsConstructor
public class DeleteRequestDto {

    private Long id;

    @NotBlank(message = "비밀번호는 필수입니다.")
    private String password;
}
