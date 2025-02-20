package com.example.newsfeed.member.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
public class DeleteRequestDto {

    private Long id;

    @NotBlank(message = "비밀번호는 필수입니다.")
    private String password;

    public DeleteRequestDto(Long id, String password) {
        this.id = id;
        this.password = password;
    }
}
