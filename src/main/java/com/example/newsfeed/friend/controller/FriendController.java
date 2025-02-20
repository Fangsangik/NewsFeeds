package com.example.newsfeed.friend.controller;

import com.example.newsfeed.auth.jwt.service.UserDetailsImpl;
import com.example.newsfeed.util.AuthenticatedMemberUtil;
import com.example.newsfeed.friend.dto.*;
import com.example.newsfeed.friend.service.FriendService;
import com.example.newsfeed.member.entity.Member;
import org.springframework.data.domain.Page;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/friends")
public class FriendController {

    private final FriendService friendService;

    public FriendController(FriendService friendService) {
        this.friendService = friendService;
    }


    @PostMapping
    public ResponseEntity<FriendResponseDto> addFriend(@AuthenticationPrincipal UserDetailsImpl userDetails,
                                                       @RequestBody FriendRequestDto friendRequestDto) {

        Member member = AuthenticatedMemberUtil.getMember(userDetails);

        FriendResponseDto response = friendService.addFriend(member, friendRequestDto);
        return ResponseEntity.ok(response);
    }

    @PatchMapping("/accept")
    public ResponseEntity<FriendResponseDto> acceptFriendRequest(@AuthenticationPrincipal UserDetailsImpl userDetails,
                                                                 @RequestBody FriendAcceptRequestDto friendAcceptRequestDto) {

        Member member = AuthenticatedMemberUtil.getMember(userDetails);
        FriendResponseDto response = friendService.acceptFriendRequest(member, friendAcceptRequestDto);
        return ResponseEntity.ok(response);
    }

    @GetMapping("/sent")
    public ResponseEntity<Page<FriendSenderRequestDto>> findSentRequests(@AuthenticationPrincipal UserDetailsImpl userDetails,
                                                                         @RequestParam (defaultValue = "0") int page, @RequestParam (defaultValue = "10") int size) {

        Member member = AuthenticatedMemberUtil.getMember(userDetails);
        Page<FriendSenderRequestDto> response = friendService.findSenderInfo(member, page, size);
        return ResponseEntity.ok(response);
    }

    @GetMapping("/received")
    public ResponseEntity<Page<FriendRequestResponseDto>> findReceivedRequests(@AuthenticationPrincipal UserDetailsImpl userDetails,
                                                                               @RequestParam (defaultValue = "0") int page, @RequestParam (defaultValue = "10") int size) {

        Member member = AuthenticatedMemberUtil.getMember(userDetails);
        Page<FriendRequestResponseDto> response = friendService.findReceivedFriendRequests(member, page, size);
        return ResponseEntity.ok(response);
    }

    @GetMapping
    public ResponseEntity<Page<FriendListDto>> findFriends(@AuthenticationPrincipal UserDetailsImpl userDetails,
                                                           @RequestParam (defaultValue = "0") int page,
                                                           @RequestParam (defaultValue = "10") int size) {

        Member member = AuthenticatedMemberUtil.getMember(userDetails);
        Page<FriendListDto> response = friendService.findFriendList(member, page, size);
        return ResponseEntity.ok(response);
    }

    @DeleteMapping("/{friendId}")
    public ResponseEntity<Void> deleteFriend (@AuthenticationPrincipal UserDetailsImpl userDetails, @PathVariable Long friendId) {

        Member member = AuthenticatedMemberUtil.getMember(userDetails);
        friendService.deleteFriend(member, friendId);
        return ResponseEntity.noContent().build();
    }
}
