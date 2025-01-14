package com.example.newsfeed.friend.controller;

import com.example.newsfeed.auth.util.AuthenticatedMemberUtil;
import com.example.newsfeed.friend.dto.*;
import com.example.newsfeed.friend.service.FriendService;
import org.springframework.data.domain.Page;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/friends")
public class FriendController {

    private final FriendService friendService;

    public FriendController(FriendService friendService) {
        this.friendService = friendService;
    }


    @PostMapping
    public ResponseEntity<FriendResponseDto> addFriend(@RequestBody FriendRequestDto friendRequestDto) {

        Long memberId = AuthenticatedMemberUtil.getAuthenticatedMemberId();

        FriendResponseDto response = friendService.addFriend(friendRequestDto, memberId);
        return ResponseEntity.ok(response);
    }

    @PatchMapping("/accept")
    public ResponseEntity<FriendResponseDto> acceptFriendRequest(@RequestBody FriendRequestDto friendRequestDto) {

        Long memberId = AuthenticatedMemberUtil.getAuthenticatedMemberId();

        FriendResponseDto response = friendService.acceptFriendRequest(friendRequestDto, memberId);
        return ResponseEntity.ok(response);
    }

    @GetMapping("/sent")
    public ResponseEntity<Page<FriendSenderResponseDto>> findSentRequests(@RequestParam (defaultValue = "0") int page, @RequestParam (defaultValue = "10") int size) {

        Long memberId = AuthenticatedMemberUtil.getAuthenticatedMemberId();

        Page<FriendSenderResponseDto> response = friendService.findSenderInfo(memberId, page, size);
        return ResponseEntity.ok(response);
    }

    @GetMapping("/received")
    public ResponseEntity<Page<FriendRequestResponseDto>> findReceivedRequests(@RequestParam (defaultValue = "0") int page, @RequestParam (defaultValue = "10") int size) {

        Long memberId = AuthenticatedMemberUtil.getAuthenticatedMemberId();
        Page<FriendRequestResponseDto> response = friendService.findReceivedFriendRequests(memberId, page, size);
        return ResponseEntity.ok(response);
    }

    @GetMapping
    public ResponseEntity<Page<FriendListDto>> findFriends(
            @RequestParam (defaultValue = "0") int page, @RequestParam (defaultValue = "10") int size) {

        Long memberId = AuthenticatedMemberUtil.getAuthenticatedMemberId();

        Page<FriendListDto> response = friendService.findFriendList(memberId, page, size);
        return ResponseEntity.ok(response);
    }

    @DeleteMapping("/{friendId}")
    public ResponseEntity<Void> deleteFriend (@PathVariable Long friendId) {

        Long memberId = AuthenticatedMemberUtil.getAuthenticatedMemberId();

        friendService.deleteFriend(friendId);
        return ResponseEntity.noContent().build();
    }
}
