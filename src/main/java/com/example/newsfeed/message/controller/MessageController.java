package com.example.newsfeed.message.controller;

import com.example.newsfeed.auth.jwt.service.UserDetailsImpl;
import com.example.newsfeed.constants.response.CommonResponse;
import com.example.newsfeed.member.entity.Member;
import com.example.newsfeed.message.dto.MessageRequestDto;
import com.example.newsfeed.message.dto.MessageResponseDto;
import com.example.newsfeed.message.service.MessageService;
import com.example.newsfeed.util.AuthenticatedMemberUtil;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/messages")
public class MessageController {

    private final MessageService messageService;

    public MessageController(MessageService messageService) {
        this.messageService = messageService;
    }

    public record SendBody(Long receiverId, String content) {}

    @PostMapping
    public ResponseEntity<CommonResponse<MessageResponseDto>> send(
            @AuthenticationPrincipal UserDetailsImpl userDetails,
            @RequestBody SendBody body) {
        Member me = AuthenticatedMemberUtil.getMember(userDetails);
        MessageResponseDto dto = messageService.sendMessage(me.getId(), body.receiverId(), body.content());
        return ResponseEntity.ok(new CommonResponse<>("메시지 전송 완료", dto));
    }

    @GetMapping("/with/{peerId:[0-9]+}")
    public ResponseEntity<CommonResponse<Page<MessageResponseDto>>> conversation(
            @AuthenticationPrincipal UserDetailsImpl userDetails,
            @PathVariable Long peerId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "50") int size) {
        Member me = AuthenticatedMemberUtil.getMember(userDetails);
        Page<MessageResponseDto> data = messageService.getConversation(me.getId(), peerId, PageRequest.of(page, size));
        return ResponseEntity.ok(new CommonResponse<>("대화 조회 완료", data));
    }

    @GetMapping("/unread")
    public ResponseEntity<CommonResponse<Page<MessageRequestDto>>> unread(
            @AuthenticationPrincipal UserDetailsImpl userDetails,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        Member me = AuthenticatedMemberUtil.getMember(userDetails);
        Page<MessageRequestDto> data = messageService.getUnread(me.getId(), PageRequest.of(page, size));
        return ResponseEntity.ok(new CommonResponse<>("안 읽은 메시지", data));
    }

    @GetMapping("/inbox")
    public ResponseEntity<CommonResponse<Page<MessageRequestDto>>> inbox(
            @AuthenticationPrincipal UserDetailsImpl userDetails,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        Member me = AuthenticatedMemberUtil.getMember(userDetails);
        Page<MessageRequestDto> data = messageService.getInbox(me.getId(), PageRequest.of(page, size));
        return ResponseEntity.ok(new CommonResponse<>("받은 메시지", data));
    }

    @PatchMapping("/{id:[0-9]+}/read")
    public ResponseEntity<CommonResponse<Map<String, Object>>> markRead(@PathVariable Long id) {
        messageService.markAsRead(id);
        return ResponseEntity.ok(new CommonResponse<>("읽음 처리 완료", Map.of("id", id)));
    }
}
