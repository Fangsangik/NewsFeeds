package com.example.newsfeed.friend.service;

import com.example.newsfeed.friend.dto.FriendListDto;
import com.example.newsfeed.friend.dto.FriendRequestDto;
import com.example.newsfeed.friend.dto.FriendResponseDto;
import com.example.newsfeed.friend.dto.FriendSenderResponseDto;
import org.springframework.data.domain.Page;

public interface FriendService {
    FriendResponseDto addFriend(Long memberId, FriendRequestDto friendRequestDto);
    Page<FriendSenderResponseDto> findSenderInfo(Long senderId, int page, int size);
    Page<FriendSenderResponseDto> findReceivedFriendRequests(Long receiverId, int page, int size);
    FriendResponseDto acceptFriendRequest(Long friendId, Long memberId);
    void deleteFriend(Long friendId);
    Page<FriendListDto> findFriendList(Long memberId, int page, int size);
}
