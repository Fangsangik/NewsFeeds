package com.example.newsfeed.message.controller;

import com.example.newsfeed.constants.response.CommonResponse;
import com.example.newsfeed.message.dto.MessageRequestDto;
import com.example.newsfeed.message.service.MessageService;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/messages")
public class MessageController {

    private final MessageService messageService;

    public MessageController(MessageService messageService) {
        this.messageService = messageService;
    }

    @PostMapping
    public ResponseEntity<CommonResponse<String>> sendMessage
            (@RequestAttribute("memberId") Long memberId,
             @RequestBody MessageRequestDto requestDto) {

        if (!memberId.equals(requestDto.getSenderId())) {
            return ResponseEntity.badRequest().body(new CommonResponse<>("본인이 보낸 메시지만 전송할 수 있습니다."));
        }

        messageService.sendMessage(requestDto);
        return ResponseEntity.ok(new CommonResponse<>("Message 전송 완료"));
    }

    @PatchMapping("/{messageId}/read")
    public ResponseEntity<CommonResponse<?>> markAsRead
            (@RequestAttribute("memberId") Long memberId,
             @PathVariable Long messageId) {
        messageService.markAsRead(messageId);
        return ResponseEntity.ok(new CommonResponse<>("읽음 표시 완료"));
    }

    @GetMapping
    public ResponseEntity<CommonResponse<Page<MessageRequestDto>>> getMessageList
            (@RequestAttribute("memberId") Long memberId,
             @RequestParam(defaultValue = "0") int page,
             @RequestParam(defaultValue = "10") int size) {
        Page<MessageRequestDto> messageByUserId = messageService.getMessages(memberId, PageRequest.of(page, size));
        return ResponseEntity.ok(new CommonResponse<>("조회 완료", messageByUserId));
    }
}
