package com.example.newsfeed.util;

import com.example.newsfeed.auth.jwt.service.UserDetailsImpl;
import com.example.newsfeed.member.entity.Member;
import org.springframework.security.core.userdetails.UserDetails;

/**
 * 인증된 사용자 정보를 가져오는 유틸리티 클래스
 *
 * 스프링 시큐리티를 사용할 경우 @AuthenticationPrincipal 어노테이션을 사용하여 인증된 사용자 정보를 가져올 수 있지만,
 * 중복 코드가 각각 Controller에 존재해, 중복 코드를 줄이기 위해 별도의 유틸리티 클래스를 생성
 */
public class AuthenticatedMemberUtil {

    public static Member getMember(UserDetails userDetails) {
        return ((UserDetailsImpl) userDetails).getMember();
    }
}
