package com.example.newsfeed.message.controller;

import com.example.newsfeed.constants.response.CommonResponse;
import com.example.newsfeed.message.dto.MessageRequestDto;
import com.example.newsfeed.message.service.MessageService;
import org.springframework.data.domain.Page;
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
            (@RequestBody MessageRequestDto requestDto) {
        messageService.sendMessage(requestDto);
        return ResponseEntity.ok(new CommonResponse<>("Message 전송 완료"));
    }

    @GetMapping
    public ResponseEntity<CommonResponse<Page<MessageRequestDto>>> getMessageList
            (@RequestAttribute("memberId") Long memberId,
             @RequestParam(defaultValue = "0") int page,
             @RequestParam(defaultValue = "10") int size) {
        Page<MessageRequestDto> messageByUserId = messageService.getMessageByUserId(memberId, page, size);
        return ResponseEntity.ok(new CommonResponse<>("조회 완료", messageByUserId));
    }
}
