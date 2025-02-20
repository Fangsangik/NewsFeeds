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
}
