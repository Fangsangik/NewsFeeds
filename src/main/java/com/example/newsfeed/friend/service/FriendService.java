package com.example.newsfeed.friend.service;

import com.example.newsfeed.friend.dto.*;
import com.example.newsfeed.member.entity.Member;
import org.springframework.data.domain.Page;

public interface FriendService {
    FriendResponseDto addFriend(Member member, FriendRequestDto friendRequestDto);
    FriendResponseDto acceptFriendRequest(Member member, FriendAcceptRequestDto friendAcceptRequestDto);
    Page<FriendSenderRequestDto> findSenderInfo(Member member, int page, int size);
    Page<FriendRequestResponseDto> findReceivedFriendRequests(Member member, int page, int size);
    void deleteFriend(Member member, Long friendId);
    Page<FriendListDto> findFriendList(Member member, int page, int size);
    String statusWith(Long meId, Long otherId);
    void deleteBetween(Long meId, Long otherId);
    long countFriends(Long memberId);
    java.util.List<com.example.newsfeed.friend.dto.FriendMemberDto> friendMembers(Long memberId);
    java.util.List<com.example.newsfeed.friend.dto.FriendSuggestionDto> suggestions(Long memberId, int limit);
}
