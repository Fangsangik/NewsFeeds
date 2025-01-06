package com.example.newsfeed.auth.jwt.service;

import com.example.newsfeed.member.entity.Member;
import com.example.newsfeed.member.type.Role;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

import java.util.ArrayList;
import java.util.Collection;

@Slf4j
public class UserDetailsImpl implements UserDetails {

    private final Member member;


    public UserDetailsImpl(Member member) {
        this.member = member;
    }


    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        Role role = this.member.getRole();
        log.info("사용자 권한 : {}", role.getAuthorities());

        return new ArrayList<>(role.getAuthorities());
    }

    /**
     * 사영자 자격 증명 반환
     * @return 암호
     */
    @Override
    public String getPassword() {
        return this.member.getPassword();
    }

    /**
     * 사용자 자격 증명 반환
     * @return 사용자 이름
     */
    @Override
    public String getUsername() {
        return this.member.getEmail();
    }

    /**
     * 계정 만료 여부 반환
     * @return 사용 여부
     */
    @Override
    public boolean isAccountNonExpired() {
        return true;
    }

    /**
     * 계정 잠금 여부 반환
     * @return 사용 여부
     */
    @Override
    public boolean isAccountNonLocked() {
        return true;
    }

    /**
     * 자격 증명 만료 여부 반환
     * @return 사용 여부
     */
    @Override
    public boolean isCredentialsNonExpired() {
        return true;
    }

    /**
     * 계정 사용 가능 여부 반환
     * @return 사용 여부
     */
    @Override
    public boolean isEnabled() {
        return true;
    }
}
