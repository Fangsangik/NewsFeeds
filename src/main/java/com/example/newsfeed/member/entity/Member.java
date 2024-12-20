package com.example.newsfeed.member.entity;

import com.example.newsfeed.auth.type.LoginType;
import com.example.newsfeed.constants.BaseEntity;
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
    private List<Message> sentMessages = new ArrayList<>();

    @OneToMany(mappedBy = "receiver", cascade = CascadeType.ALL)
    private List<Message> receivedMessages = new ArrayList<>();


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

    public Member(Long memberId) {
        this.id = memberId;
    }

    public void markAsDeleted() {
        this.deletedAt = LocalDateTime.now();
    }

    public void updatedPassword(String newPassword) {
        this.password = newPassword;
    }
}

