package com.example.newsfeed.auth.util;

import com.example.newsfeed.auth.jwt.service.UserDetailsImpl;
import com.example.newsfeed.exception.ErrorCode;
import com.example.newsfeed.exception.NoAuthorizedException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;

/**
 * 인증된 사용자 정보를 가져오는 유틸리티 클래스
 *
 * 스프링 시큐리티를 사용할 경우 @AuthenticationPrincipal 어노테이션을 사용하여 인증된 사용자 정보를 가져올 수 있지만,
 * 중복 코드가 각각 Controller에 존재해, 중복 코드를 줄이기 위해 별도의 유틸리티 클래스를 생성
 */
public class AuthenticatedMemberUtil {

    public AuthenticatedMemberUtil() {
    }

    public static Long getAuthenticatedMemberId() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();

        if (authentication == null || !authentication.isAuthenticated()) {
            throw new NoAuthorizedException(ErrorCode.NO_AUTHOR);
        }

        Object principal = authentication.getPrincipal();
        if (!(principal instanceof UserDetailsImpl)) {
            throw new NoAuthorizedException(ErrorCode.NO_AUTHOR);
        }

        UserDetailsImpl userDetails = (UserDetailsImpl) principal;
        Long memberId = userDetails.getMemberId();

        if (memberId == null) {
            throw new NoAuthorizedException(ErrorCode.NO_AUTHOR);
        }

        return memberId;
    }
}
