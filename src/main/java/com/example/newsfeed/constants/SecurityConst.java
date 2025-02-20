package com.example.newsfeed.constants;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public class SecurityConst {
    public static final String[] WHITE_LIST = {"/members/signup", "/auth/**", "/auth/login", "/auth/logout", "/kakao/**", "/v3/api-docs/**", "/swagger-ui/**", "/swagger-ui.html"};
    public static final String[] ADMIN_INTERCEPTOR_LIST = {};
    public static final String[] USER_INTERCEPTOR_LIST = {"/members/**", "/feeds/**", "/comments/**", "/likes/**", "/friends/**"};
}