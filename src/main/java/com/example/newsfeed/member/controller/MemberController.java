package com.example.newsfeed.member.controller;

import com.example.newsfeed.auth.jwt.service.UserDetailsImpl;
import com.example.newsfeed.util.AuthenticatedMemberUtil;
import com.example.newsfeed.constants.response.CommonResponse;
import com.example.newsfeed.member.dto.*;
import com.example.newsfeed.member.entity.Member;
import com.example.newsfeed.member.service.MemberService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/members")
public class MemberController {

    private final MemberService memberService;
    private final com.example.newsfeed.member.repository.MemberRepository memberRepository;

    public MemberController(MemberService memberService,
                            com.example.newsfeed.member.repository.MemberRepository memberRepository) {
        this.memberService = memberService;
        this.memberRepository = memberRepository;
    }

    // 비공개 계정 토글
    @org.springframework.transaction.annotation.Transactional
    @PatchMapping("/privacy")
    public ResponseEntity<CommonResponse<java.util.Map<String, Object>>> togglePrivacy() {
        Long me = AuthenticatedMemberUtil.getAuthenticatedMemberId();
        Member m = memberRepository.findByIdOrElseThrow(me);
        m.setPrivate(!m.isPrivate());
        return ResponseEntity.ok(new CommonResponse<>("공개범위 변경", java.util.Map.of("isPrivate", m.isPrivate())));
    }

    @GetMapping("/search")
    public ResponseEntity<CommonResponse<org.springframework.data.domain.Page<com.example.newsfeed.member.dto.MemberSearchDto>>> searchMembers(
            @org.springframework.web.bind.annotation.RequestParam(name = "q", defaultValue = "") String q,
            @org.springframework.web.bind.annotation.RequestParam(defaultValue = "0") int page,
            @org.springframework.web.bind.annotation.RequestParam(defaultValue = "10") int size) {
        var pageable = org.springframework.data.domain.PageRequest.of(page, size);
        var result = (q == null || q.isBlank())
                ? org.springframework.data.domain.Page.<com.example.newsfeed.member.dto.MemberSearchDto>empty(pageable)
                : memberService.searchMembers(q.trim(), pageable);
        return ResponseEntity.ok(new CommonResponse<>("회원 검색 완료", result));
    }

    @GetMapping("/{id:[0-9]+}")
    public ResponseEntity<CommonResponse<MemberResponseDto>> getProfile(@PathVariable Long id) {
        MemberResponseDto memberById = memberService.getMemberById(id);
        return ResponseEntity.ok(new CommonResponse<>("프로필 조회 완료", memberById));
    }

    @PostMapping("/signup")
    public ResponseEntity<CommonResponse<MemberResponseDto>> createMember(@Valid @RequestBody MemberRequestDto memberDto) {
        MemberResponseDto createdMember = memberService.createMember(memberDto);
        return ResponseEntity.ok(new CommonResponse<>("회원가입 완료", createdMember));
    }

    @PutMapping("/update")
    public ResponseEntity<CommonResponse<MemberUpdateResponseDto>> updateMember(@AuthenticationPrincipal UserDetailsImpl userDetails,
                                                                                @Valid @RequestBody MemberUpdateRequestDto requestDto) {
        Member member = AuthenticatedMemberUtil.getMember(userDetails);
        MemberUpdateResponseDto responseDto = memberService.updateMember(member, requestDto);
        return ResponseEntity.ok(new CommonResponse<>("회원 정보 수정 완료", responseDto));
    }

    @GetMapping("/{feedId:[0-9]+}/member")
    public ResponseEntity<CommonResponse<MemberResponseDto>> getMemberByFeedId(@PathVariable Long feedId) {
        MemberResponseDto member = memberService.getMemberByFeedId(feedId);
        return ResponseEntity.ok(new CommonResponse<>("피드 작성자 조회 완료", member));
    }

    @PutMapping("/password")
    public ResponseEntity<CommonResponse<String>> changePassword(@AuthenticationPrincipal UserDetailsImpl userDetails,
                                                                 @Valid @RequestBody PasswordRequestDto passwordRequestDto) {
        Member member = AuthenticatedMemberUtil.getMember(userDetails);
        memberService.changePassword(passwordRequestDto.getOldPassword(), passwordRequestDto.getNewPassword(), member);
        return ResponseEntity.ok(new CommonResponse<>("비밀번호 변경 완료", "성공적으로 비밀번호를 변경했습니다."));
    }

    @DeleteMapping("/delete")
    public ResponseEntity<CommonResponse<String>> deleteMemberById(@AuthenticationPrincipal UserDetailsImpl userDetails,
                                                                   @Valid @RequestBody DeleteRequestDto deleteRequestDto) {
        Member member = AuthenticatedMemberUtil.getMember(userDetails);
        memberService.deleteMemberById(member, deleteRequestDto.getPassword());
        return ResponseEntity.ok(new CommonResponse<>("회원 삭제 완료", "성공적으로 탈퇴 처리되었습니다."));
    }
}
