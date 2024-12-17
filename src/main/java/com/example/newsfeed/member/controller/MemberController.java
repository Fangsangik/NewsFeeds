package com.example.newsfeed.member.controller;

import com.example.newsfeed.constants.response.CommonResponse;
import com.example.newsfeed.member.dto.*;
import com.example.newsfeed.member.service.MemberService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/members")
public class MemberController {

    private final MemberService memberService;

    public MemberController(MemberService memberService) {
        this.memberService = memberService;
    }

    @GetMapping("/{id}")
    public ResponseEntity<CommonResponse<MemberResponseDto>> getProfile(@PathVariable Long id) {
        MemberResponseDto memberById = memberService.getMemberById(id);
        return ResponseEntity.ok(new CommonResponse<>("프로필 조회 완료", memberById));
    }

    @PostMapping("/signup")
    public ResponseEntity<CommonResponse<MemberLoginResponseDto>> createMember(@Valid @RequestBody MemberRequestDto memberDto) {
        MemberLoginResponseDto createdMember = memberService.createMember(memberDto);
        return ResponseEntity.ok(new CommonResponse<>("회원가입 완료", createdMember));
    }

    @PutMapping("/update")
    public ResponseEntity<CommonResponse<MemberUpdateResponseDto>> updateMember(
            @RequestAttribute(name = "userId") Long userId, // JWT 필터를 통해 userId 제공
            @Valid @RequestBody MemberUpdateRequestDto requestDto
    ) {
        MemberUpdateResponseDto responseDto = memberService.updateMember(userId, requestDto);
        return ResponseEntity.ok(new CommonResponse<>("회원 정보 수정 완료", responseDto));
    }

    @GetMapping("/{feedId}/member")
    public ResponseEntity<CommonResponse<MemberResponseDto>> getMemberByFeedId(@PathVariable Long feedId) {
        MemberResponseDto member = memberService.getMemberByFeedId(feedId);
        return ResponseEntity.ok(new CommonResponse<>("피드 작성자 조회 완료", member));
    }

    @PutMapping("/password")
    public ResponseEntity<CommonResponse<String>> changePassword(
            @RequestAttribute(name = "userId") Long userId, // JWT 필터를 통해 userId 제공
            @Valid @RequestBody PasswordRequestDto passwordRequestDto
    ) {
        memberService.changePassword(passwordRequestDto.getOldPassword(), passwordRequestDto.getNewPassword(), userId);
        return ResponseEntity.ok(new CommonResponse<>("비밀번호 변경 완료", "성공적으로 비밀번호를 변경했습니다."));
    }

    @DeleteMapping("/delete")
    public ResponseEntity<CommonResponse<String>> deleteMemberById(
            @RequestAttribute(name = "userId") Long userId, // JWT 필터를 통해 userId 제공
            @Valid @RequestBody DeleteRequestDto deleteRequestDto
    ) {
        memberService.deleteMemberById(userId, deleteRequestDto.getPassword());
        return ResponseEntity.ok(new CommonResponse<>("회원 삭제 완료", "성공적으로 탈퇴 처리되었습니다."));
    }
}
