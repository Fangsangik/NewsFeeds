package com.example.newsfeed.auth.dto;

import lombok.Builder;
import lombok.Getter;

@Getter
public class AuthLoginDto {
    private Long id;
    private String email;


    @Builder
    public AuthLoginDto(Long id, String email) {
        this.id = id;
        this.email = email;
    }

}
