package com.example.newsfeed.member.service;

import com.example.newsfeed.member.dto.*;

public interface MemberService {
    MemberLoginResponseDto createMember(MemberRequestDto requestDto);
    MemberUpdateResponseDto updateMember(Long memberId, MemberUpdateRequestDto requestDto);
    MemberResponseDto getMemberById(Long id);
    void deleteMemberById(Long id, String password);
    MemberResponseDto changePassword(String oldPassword, String newPassword, Long memberId);
    MemberResponseDto getMemberByFeedId(Long feedId);
    Long findOrCreateMember(Long kakaoId, String email, String nickname);
}
