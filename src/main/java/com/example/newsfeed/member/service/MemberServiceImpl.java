package com.example.newsfeed.member.service;

import com.example.newsfeed.auth.jwt.JwtProvider;
import com.example.newsfeed.auth.type.LoginType;
import com.example.newsfeed.config.PasswordEncoder;
import com.example.newsfeed.exception.*;
import com.example.newsfeed.kakao.entity.KakaoMember;
import com.example.newsfeed.kakao.repository.KakaoMemberRepository;
import com.example.newsfeed.member.dto.*;
import com.example.newsfeed.member.entity.Member;
import com.example.newsfeed.member.repository.MemberRepository;
import com.example.newsfeed.member.type.Role;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Map;

import static com.example.newsfeed.exception.ErrorCode.*;


@Slf4j
@Service
public class MemberServiceImpl implements MemberService {

    private final JwtProvider jwtProvider;
    private final MemberRepository memberRepository;
    private final KakaoMemberRepository kakaoMemberRepository;
    private final PasswordEncoder passwordEncoder;

    public MemberServiceImpl(JwtProvider jwtProvider, MemberRepository memberRepository, KakaoMemberRepository kakaoMemberRepository, PasswordEncoder passwordEncoder) {
        this.jwtProvider = jwtProvider;
        this.memberRepository = memberRepository;
        this.kakaoMemberRepository = kakaoMemberRepository;
        this.passwordEncoder = passwordEncoder;
    }

    @Override
    @Transactional
    public MemberLoginResponseDto createMember(MemberRequestDto memberDto) {
        // 이메일 중복 검사
        log.info("회원 중복 검사 : {}", memberDto.getEmail());
        if (memberRepository.existsByEmail(memberDto.getEmail())) {
            throw new DuplicatedException(EMAIL_EXIST);
        }

        // 비밀번호 암호화 및 회원 생성
        String encodedPassword = passwordEncoder.encode(memberDto.getPassword());
        log.debug("암호화된 비밀번호: {}", memberDto.getPassword());

        Member newMember = MemberRequestDto.toEntity(memberDto, encodedPassword);
        log.debug("Entity 생성 후 비밀번호: {}", newMember.getPassword());

        Member savedMember = memberRepository.save(newMember);
        log.debug("저장된 Member 비밀번호: {}", savedMember.getPassword());

        // JWT 토큰 생성
        Map<String, String> tokens = jwtProvider.generateTokens(
                savedMember.getId(),
                savedMember.getRole(),
                LoginType.NORMAL_USER
        );

        // 토큰 정보를 포함한 DTO 반환
        return MemberLoginResponseDto.builder()
                .id(savedMember.getId())
                .accessToken(tokens.get("accessToken"))
                .refreshToken(tokens.get("refreshToken"))
                .build();
    }

    @Override
    @Transactional
    public MemberUpdateResponseDto updateMember(Long userId, MemberUpdateRequestDto requestDto) {
        log.info("회원 update 유효성 검사 : {}", userId);

        Member member = memberRepository.findById(userId)
                .orElseThrow(() -> new NotFoundException(ErrorCode.NOT_FOUND_MEMBER));

        // 비밀번호 검증
        if (!passwordEncoder.matches(requestDto.getPassword(), member.getPassword())) {
            throw new InvalidInputException(WRONG_PASSWORD);
        }

        // 회원 정보 업데이트
        requestDto.updatedMember(requestDto);

        // 저장 후 업데이트된 데이터 반환
        Member updatedMember = memberRepository.save(member);
        return MemberUpdateResponseDto.toResponseDto(updatedMember);
    }

    @Transactional
    @Override
    public Long findOrCreateMember(Long kakaoId, String email, String nickname) {
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
        Member member = memberRepository.findByKakaoMember(kakaoMember)
                .orElseGet(() -> {
                    Member newMember = Member.builder()
                            .name(nickname)
                            .email(email)
                            .role(Role.USER)
                            .kakaoMember(kakaoMember)
                            .build();
                    return memberRepository.save(newMember);
                });

        // 최종적으로 Member의 ID 반환
        return member.getId();
    }
    @Override
    @Transactional(readOnly = true)
    public MemberResponseDto getMemberById(Long id) {
        return new MemberResponseDto(
                memberRepository.findByIdAndNotDeleted(id).orElseThrow(() -> new IllegalArgumentException("회원을 찾을 수 없습니다."))
        );
    }


    @Transactional(readOnly = true)
    // 피드 아이디 기반 회원 조회
    public MemberResponseDto getMemberByFeedId(Long feedId) {
        // 피드 ID로 작성자(Member)를 조회
        Member member = memberRepository.findMemberByFeedId(feedId)
                .orElseThrow(() -> new IllegalArgumentException("Member not found for feedId: " + feedId));

        // Member 엔터티를 MemberResponseDto로 변환하여 반환
        return MemberResponseDto.toDto(member);
    }

    @Override
    @Transactional
    public void deleteMemberById(Long id, String password) {
        Member member = memberRepository.findById(id)
                .orElseThrow(() -> new NotFoundException(NOT_FOUND_MEMBER));

        if (!passwordEncoder.matches(password, member.getPassword())) {
            throw new InvalidInputException(WRONG_PASSWORD);
        }

        member.markAsDeleted(); // 소프트 삭제
        memberRepository.save(member);
    }

    @Override
    @Transactional
    public MemberResponseDto changePassword(String oldPassword, String newPassword, Long memberId) {
        // 세션에서 PK 기반으로 조회
        if (memberId == null) {
            throw new InternalServerException(SESSION_TIMEOUT);
        }

        Member member = memberRepository.findById(memberId)
                .orElseThrow(() -> new NotFoundException(NOT_FOUND_MEMBER));

        if (!passwordEncoder.matches(oldPassword, member.getPassword())) {
            throw new InvalidInputException(WRONG_PASSWORD);
        }

        if (oldPassword.equals(newPassword)) {
            throw new InvalidInputException(SAME_PASSWORD);
        }

        //비밀번호 update
        member.updatedPassword(passwordEncoder.encode(newPassword));
        Member updatedMember = memberRepository.save(member);

        // 비밀번호 변경 후 세션 무효화
        return MemberResponseDto.toDto(updatedMember);
    }
}