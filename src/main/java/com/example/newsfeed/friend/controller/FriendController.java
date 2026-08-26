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

    // 특정 회원과의 관계 상태 (self|friends|requested_by_me|requested_to_me|none)
    @GetMapping("/status/{memberId}")
    public ResponseEntity<java.util.Map<String, String>> statusWith(@AuthenticationPrincipal UserDetailsImpl userDetails,
                                                                    @PathVariable Long memberId) {
        Member me = AuthenticatedMemberUtil.getMember(userDetails);
        return ResponseEntity.ok(java.util.Map.of("status", friendService.statusWith(me.getId(), memberId)));
    }

    // 회원 기준 친구 관계 해제(방향 무관)
    @DeleteMapping("/by-member/{memberId}")
    public ResponseEntity<Void> deleteByMember(@AuthenticationPrincipal UserDetailsImpl userDetails,
                                               @PathVariable Long memberId) {
        Member me = AuthenticatedMemberUtil.getMember(userDetails);
        friendService.deleteBetween(me.getId(), memberId);
        return ResponseEntity.noContent().build();
    }

    // 특정 회원의 친구 수 (상호 모델 → 팔로워=팔로잉)
    @GetMapping("/count/{memberId}")
    public ResponseEntity<java.util.Map<String, Long>> friendCount(@PathVariable Long memberId) {
        return ResponseEntity.ok(java.util.Map.of("friends", friendService.countFriends(memberId)));
    }

    // 특정 회원의 친구(팔로워/팔로잉) 목록
    @GetMapping("/members/{memberId}")
    public ResponseEntity<java.util.List<com.example.newsfeed.friend.dto.FriendMemberDto>> friendMembers(@PathVariable Long memberId) {
        return ResponseEntity.ok(friendService.friendMembers(memberId));
    }
}
