package com.example.newsfeed.friend.service;

import com.example.newsfeed.friend.dto.*;
import org.springframework.data.domain.Page;

public interface FriendService {
    FriendResponseDto addFriend(FriendRequestDto friendRequestDto, Long authenticatedMemberId);
    FriendResponseDto acceptFriendRequest(FriendRequestDto friendRequestDto, Long authenticatedMemberId);
    Page<FriendSenderResponseDto> findSenderInfo(Long senderId, int page, int size);
    Page<FriendRequestResponseDto> findReceivedFriendRequests(Long receiverId, int page, int size);
    void deleteFriend(Long friendId);
    Page<FriendListDto> findFriendList(Long memberId, int page, int size);
}
