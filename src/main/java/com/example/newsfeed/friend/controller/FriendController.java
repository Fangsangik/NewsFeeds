package com.example.newsfeed.friend.controller;

import com.example.newsfeed.constants.response.CommonResponse;
import com.example.newsfeed.friend.dto.FriendListDto;
import com.example.newsfeed.friend.dto.FriendRequestDto;
import com.example.newsfeed.friend.dto.FriendResponseDto;
import com.example.newsfeed.friend.dto.FriendSenderResponseDto;
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

    @PostMapping("/add")
    public ResponseEntity<CommonResponse<FriendResponseDto>> addFriend
            (@RequestAttribute(name = "memberId") Long memberId,
             @RequestBody FriendRequestDto friendRequestDto) {
        FriendResponseDto friend = friendService.addFriend(memberId, friendRequestDto);
        return ResponseEntity.ok(new CommonResponse<>("친구 신청 완료", friend));
    }

    @PostMapping("/accept")
    public ResponseEntity<CommonResponse<FriendResponseDto>> acceptFriend
            (@RequestAttribute(name = "memberId") Long memberId,
             @RequestParam Long friendId) {
        FriendResponseDto friend = friendService.acceptFriendRequest(friendId, memberId);
        return ResponseEntity.ok(new CommonResponse<>("친구 수락 완료", friend));
    }

    @GetMapping("/senders/{senderId}")
    public ResponseEntity<CommonResponse<Page<FriendSenderResponseDto>>> getSenderInfoList
            (@RequestAttribute(name = "memberId") Long memberId,
             @PathVariable Long senderId,
             @RequestParam(value = "page", required = false, defaultValue = "0") int page,
             @RequestParam(value = "size", required = false, defaultValue = "10") int size) {
        Page<FriendSenderResponseDto> friendRequest = friendService.findSenderInfo(senderId, page, size);
        return ResponseEntity.ok(new CommonResponse<>("친구 요청 아이디 조회", friendRequest));
    }

    @GetMapping("/receivers/{receiverId}")
    public ResponseEntity<CommonResponse<Page<FriendSenderResponseDto>>> getRequestedInfo
            (@RequestAttribute(name = "memberId") Long memberId,
             @PathVariable Long receiverId,
             @RequestParam(value = "page", required = false, defaultValue = "0") int page,
             @RequestParam(value = "size", required = false, defaultValue = "10") int size) {
        Page<FriendSenderResponseDto> friendRequest = friendService.findReceivedFriendRequests(receiverId, page, size);
        return ResponseEntity.ok(new CommonResponse<>("친구 요청 받은사람 조회", friendRequest));
    }

    @GetMapping("/members/{memberId}")
    public ResponseEntity<CommonResponse<Page<FriendListDto>>> getFriendList
            (
             @PathVariable Long memberId,
             @RequestParam(value = "page", required = false, defaultValue = "0") int page,
             @RequestParam(value = "size", required = false, defaultValue = "10") int size) {
        Page<FriendListDto> friendList = friendService.findFriendList(memberId, page, size);
        return ResponseEntity.ok(new CommonResponse<>("친구 목록 조회", friendList));
    }

    @DeleteMapping("/{friendId}")
    public ResponseEntity<CommonResponse<Page<Void>>> deleteComment
            (@RequestAttribute(name = "memberId") Long memberId,
             @PathVariable Long friendId) {
        friendService.deleteFriend(friendId);
        return ResponseEntity.ok(new CommonResponse<>("친구 삭제 완료"));
    }
}
