package com.example.newsfeed.member.service;

import com.example.newsfeed.auth.jwt.dto.JwtMemberDto;
import com.example.newsfeed.member.type.LoginType;
import com.example.newsfeed.exception.*;
import com.example.newsfeed.kakao.entity.KakaoMember;
import com.example.newsfeed.kakao.repository.KakaoMemberRepository;
import com.example.newsfeed.member.dto.*;
import com.example.newsfeed.member.entity.Member;
import com.example.newsfeed.member.repository.MemberRepository;
import com.example.newsfeed.member.type.Role;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;


import static com.example.newsfeed.exception.ErrorCode.*;


@Slf4j
@Service
public class MemberServiceImpl implements MemberService {

    private final MemberRepository memberRepository;
    private final KakaoMemberRepository kakaoMemberRepository;
    private final PasswordEncoder passwordEncoder;

    public MemberServiceImpl(MemberRepository memberRepository, KakaoMemberRepository kakaoMemberRepository, PasswordEncoder passwordEncoder) {
        this.memberRepository = memberRepository;
        this.kakaoMemberRepository = kakaoMemberRepository;
        this.passwordEncoder = passwordEncoder;
    }

    @Override
    @Transactional
    public MemberResponseDto createMember(MemberRequestDto memberDto) {
        // 이메일 중복 검사
        log.info("회원 중복 검사 : {}", memberDto.getEmail());
        if (memberRepository.existsByEmail(memberDto.getEmail())) {
            throw new DuplicatedException(EMAIL_EXIST);
        }

        // 비밀번호 암호화 및 회원 생성
        String encodedPassword = passwordEncoder.encode(memberDto.getPassword());
        log.debug("암호화된 비밀번호: {}", memberDto.getPassword());

        Member newMember = MemberRequestDto.toEntity(memberDto, encodedPassword);
        memberRepository.save(newMember);

        log.info("회원 생성 완료: {}", newMember.getEmail());

        return MemberResponseDto.toDto(newMember);
    }

    @Override
    @Transactional
    public MemberUpdateResponseDto updateMember(Member member, MemberUpdateRequestDto requestDto) {
        Member findMember = memberRepository.findByIdOrElseThrow(member.getId());

        // 비밀번호 검증
        if (!passwordEncoder.matches(requestDto.getPassword(), findMember.getPassword())) {
            throw new InvalidInputException(WRONG_PASSWORD);
        }

        // 회원 정보 업데이트
        findMember.updateMember(requestDto.getName(), requestDto.getPhoneNumber(), requestDto.getAddress(), requestDto.getImage());

        // 저장 후 업데이트된 데이터 반환
        return MemberUpdateResponseDto.toResponseDto(findMember);
    }

    @Transactional
    @Override
    public JwtMemberDto findOrCreateMember(Long kakaoId, String email, String nickname) {
        // KakaoMember 조회 또는 생성
        KakaoMember kakaoMember = kakaoMemberRepository.findByKakaoId(kakaoId)
                .orElseGet(() -> {
                    KakaoMember newKakaoMember = KakaoMember.builder()
                            .kakaoId(kakaoId)
                            .email(email)
                            .nickname(nickname)
                            .build();
                    return kakaoMemberRepository.save(newKakaoMember);
                });

        // Member 조회 또는 생성
        Member member =  memberRepository.findByKakaoMember(kakaoMember)
                .orElseGet(() -> {
                    Member newMember = Member.builder()
                            .name(nickname)
                            .email(email)
                            .role(Role.USER)
                            .kakaoMember(kakaoMember)
                            .build();
                    return memberRepository.save(newMember);
                });

        return new JwtMemberDto(member.getId(), member.getEmail(), member.getRole(), LoginType.KAKAO_USER);
    }

    @Override
    public MemberResponseDto getMemberById(Long id) {
        return new MemberResponseDto(
                memberRepository.findByIdAndNotDeleted(id).orElseThrow(() -> new NotFoundException(NOT_FOUND_MEMBER))
        );
    }


    @Transactional(readOnly = true)
    // 피드 아이디 기반 회원 조회
    public MemberResponseDto getMemberByFeedId(Long feedId) {
        // 피드 ID로 작성자(Member)를 조회
        Member member = memberRepository.findMemberByFeedId(feedId)
                .orElseThrow(() -> new NotFoundException(NOT_FOUND_POST));

        // Member 엔터티를 MemberResponseDto로 변환하여 반환
        return MemberResponseDto.toDto(member);
    }

    @Transactional
    @Override
    public void deleteMemberById(Member member, String password) {
        Member findMember = memberRepository.findByIdOrElseThrow(member.getId());

        if (!passwordEncoder.matches(password, findMember.getPassword())) {
            throw new InvalidInputException(WRONG_PASSWORD);
        }

        findMember.markAsDeleted(); // 소프트 삭제
        memberRepository.save(member);
    }

    @Transactional
    @Override
    public MemberResponseDto changePassword(String oldPassword, String newPassword, Member member) {
        // 세션에서 PK 기반으로 조회
        if (member.getId() == null) {
            throw new InternalServerException(SESSION_TIMEOUT);
        }

        Member findMember = memberRepository.findByIdOrElseThrow(member.getId());

        if (!passwordEncoder.matches(oldPassword, findMember.getPassword())) {
            throw new InvalidInputException(WRONG_PASSWORD);
        }

        if (oldPassword.equals(newPassword)) {
            throw new InvalidInputException(SAME_PASSWORD);
        }

        //비밀번호 update
        findMember.updatedPassword(passwordEncoder.encode(newPassword));
        // 비밀번호 변경 후 세션 무효화
        return MemberResponseDto.toDto(findMember);
    }
}