package com.example.newsfeed.member.dto;

import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
public class AuthLoginDto {
    private Long id;
    private String email;


    @Builder
    public AuthLoginDto(Long id, String email) {
        this.id = id;
        this.email = email;
    }

}
