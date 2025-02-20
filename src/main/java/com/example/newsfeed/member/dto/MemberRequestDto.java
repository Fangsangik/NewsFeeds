package com.example.newsfeed.member.dto;

import com.example.newsfeed.member.type.LoginType;
import com.example.newsfeed.member.entity.Member;
import com.example.newsfeed.member.type.Role;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.validator.constraints.Length;

import java.time.LocalDateTime;

@Getter
@NoArgsConstructor
public class MemberRequestDto {

    @NotBlank(message = "이름은 필수 값 입니다.")
    private String name;

    @NotBlank(message = "이메일은 필수 값 입니다.")
    @Pattern(regexp = "[a-zA-Z0-9_!#$%&’*+/=?`{|}~^.-]+@[a-zA-Z0-9.-]+\\.[a-zA-Z0-9.-]+$", message = "이메일 형식이 일치하지 않습니다.")
    private String email;

    @NotBlank(message = "비밀번호은 필수 값 입니다.")
    @Length(min = 4, message = "비밀번호는 최소 4자입니다.")
    private String password;

    @NotBlank(message = "핸드폰 번호는 필수 값 입니다.")
    private String phoneNumber;

    @NotBlank(message = "주소는 필수 값 입니다.")
    private String address;

    @NotNull(message = "나이는 필수 값 입니다.")
    private int age;

    @NotNull(message = "역할은 필수 값 입니다.")
    private Role role;

    private LoginType loginType;

    private String image;
    private LocalDateTime createdAt;

    @Builder
    public MemberRequestDto(String name, String email, String password, String phoneNumber, String address, int age, String image, LocalDateTime createdAt, Role role, LoginType loginType) {
        this.name = name;
        this.email = email;
        this.password = password;
        this.phoneNumber = phoneNumber;
        this.address = address;
        this.age = age;
        this.image = image;
        this.createdAt = createdAt;
        this.role = role;
        this.loginType = loginType;
    }

    // Entity에서 DTO로 변환하는 생성자
    public static MemberRequestDto toDto(Member member) {
        return MemberRequestDto.builder()
                .name(member.getName())
                .email(member.getEmail())
                .password(member.getPassword())
                .phoneNumber(member.getPhoneNumber())
                .address(member.getAddress())
                .age(member.getAge())
                .image(member.getImage())
                .createdAt(LocalDateTime.now())
                .build();
    }

    public static Member toEntity(MemberRequestDto memberRequestDto, String password) {
        return Member.builder()
                .name(memberRequestDto.getName())
                .email(memberRequestDto.getEmail())
                .password(password)
                .phoneNumber(memberRequestDto.getPhoneNumber())
                .address(memberRequestDto.getAddress())
                .age(memberRequestDto.getAge())
                .image(memberRequestDto.getImage())
                .role(Role.USER)
                .build();
    }

}
