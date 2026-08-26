package com.example.newsfeed.member.entity;

import com.example.newsfeed.member.type.LoginType;
import com.example.newsfeed.constants.entity.BaseEntity;
import com.example.newsfeed.friend.entity.Friend;
import com.example.newsfeed.kakao.entity.KakaoMember;
import com.example.newsfeed.member.type.Role;
import com.example.newsfeed.message.entity.Message;
import jakarta.persistence.*;
import lombok.Builder;
import lombok.Getter;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Getter
public class Member extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    private String name;
    private String email;
    private String password;
    private String phoneNumber;
    private String address;
    private int age;
    private String image;

    // 비공개 계정: true면 친구(수락된)만 게시물 조회 가능
    @Column(nullable = false)
    private boolean isPrivate = false;

    @Enumerated(EnumType.STRING)
    private Role role = Role.USER;

    @Enumerated(EnumType.STRING)
    private LoginType loginType;

    private LocalDateTime deletedAt;

    //낙관적 락
    //버전 관리를 통해 동시성 충돌 감지.
//    @Version
//    private Integer version;

    @OneToMany(mappedBy = "sender", cascade = CascadeType.ALL)
    private List<Friend> friendsSent = new ArrayList<>();

    // 친구 요청을 받은 경우
    @OneToMany(mappedBy = "receiver", cascade = CascadeType.ALL)
    private List<Friend> friendsReceived = new ArrayList<>();

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "kakao_member_id")
    private KakaoMember kakaoMember;

    @OneToMany(mappedBy = "sender", cascade = CascadeType.ALL)
    private List<Message> messages = new ArrayList<>();


    @Builder
    public Member(String name, String email, String password, String phoneNumber, String address, int age, String image, LocalDateTime deletedAt, Role role, LoginType loginType, KakaoMember kakaoMember) {
        this.name = name;
        this.email = email;
        this.password = password;
        this.phoneNumber = phoneNumber;
        this.address = address;
        this.age = age;
        this.image = image;
        this.deletedAt = deletedAt;
        this.role = role;
        this.loginType = loginType;
        this.kakaoMember = kakaoMember;
    }

    public Member() {
    }

    public void setPrivate(boolean isPrivate) {
        this.isPrivate = isPrivate;
    }

    public Member(Long memberId) {
        this.id = memberId;
    }

    /**
     * JWT claims만으로 구성한 경량 인증 주체(principal).
     * 매 요청 DB 조회 없이 id/email/role만 채운다. 나머지 필드가 필요한 서비스는
     * memberId로 재조회하는 기존 패턴을 그대로 사용한다.
     */
    public static Member fromClaims(Long id, String email, Role role) {
        Member m = new Member(id);
        m.email = email;
        m.role = role;
        return m;
    }

    public void markAsDeleted() {
        this.deletedAt = LocalDateTime.now();
    }

    public void updatedPassword(String newPassword) {
        this.password = newPassword;
    }

    public void updateMember(String name, String phoneNumber, String address, String image) {
        this.name = name;
        this.phoneNumber = phoneNumber;
        this.address = address;
        this.image = image;
    }
}

