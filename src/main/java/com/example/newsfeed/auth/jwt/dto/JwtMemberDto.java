package com.example.newsfeed.auth.jwt.dto;

import com.example.newsfeed.auth.type.LoginType;
import com.example.newsfeed.member.type.Role;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@AllArgsConstructor
public class JwtMemberDto {
    private Long id;
    private String email;
    private Role role;
    private LoginType loginType;
}
