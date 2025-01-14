package com.example.newsfeed.member.type;

import lombok.Getter;
import org.springframework.security.core.authority.SimpleGrantedAuthority;

import java.util.List;

@Getter
public enum Role {
    ADMIN("관리자"),
    USER("사용자");

    private String name;

    Role(String name) {
        this.name = name;
    }

    public static Role of(String name) {
        for (Role role : Role.values()) {
            if (role.name.equals(name)) {
                return role;
            }
        }
        throw new IllegalArgumentException("해당하는 권한이 없습니다." + name);
    }


    /**
     * UserDetails에 담길 권한을 return
     * @return
     */
    public List<SimpleGrantedAuthority> getAuthorities() {
        return List.of(new SimpleGrantedAuthority("ROLE_" + this.name));
    }
}
