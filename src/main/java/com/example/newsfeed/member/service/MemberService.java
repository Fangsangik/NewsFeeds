package com.example.newsfeed.member.service;

import com.example.newsfeed.auth.jwt.dto.JwtMemberDto;
import com.example.newsfeed.member.dto.*;
import com.example.newsfeed.member.entity.Member;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

public interface MemberService {
    MemberResponseDto createMember(MemberRequestDto memberDto);
    MemberUpdateResponseDto updateMember(Member member, MemberUpdateRequestDto requestDto);
    MemberResponseDto getMemberById(Long id);
    void deleteMemberById(Member member, String password);
    MemberResponseDto changePassword(String oldPassword, String newPassword, Member member);
    MemberResponseDto getMemberByFeedId(Long feedId);
    JwtMemberDto findOrCreateMember(Long kakaoId, String email, String nickname);
    Page<MemberSearchDto> searchMembers(String q, Pageable pageable);
}
