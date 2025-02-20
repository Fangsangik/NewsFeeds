package com.example.newsfeed.message.contorller;

import com.example.newsfeed.auth.jwt.service.UserDetailsImpl;
import com.example.newsfeed.util.AuthenticatedMemberUtil;
import com.example.newsfeed.constants.response.CommonResponse;
import com.example.newsfeed.member.entity.Member;
import com.example.newsfeed.message.dto.MessageRequestDto;
import com.example.newsfeed.message.dto.MessageResponseDto;
import com.example.newsfeed.message.service.MessageService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.http.ResponseEntity;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.handler.annotation.SendTo;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@Slf4j
@RestController
@RequiredArgsConstructor
public class MessageController {

    private final MessageService messageService;

    @MessageMapping("/chat.sendMessage")
    @SendTo("/topic/public")
    public ResponseEntity<CommonResponse<MessageResponseDto>> sendMessage(@AuthenticationPrincipal UserDetailsImpl userDetails,
                                                                           MessageRequestDto messageRequestDto) {
        Member member = AuthenticatedMemberUtil.getMember(userDetails);
        log.info("✉️ 메시지 발송: {}", messageRequestDto.getContent());
        MessageResponseDto messageResponseDto = messageService.sendMessage(member, messageRequestDto);
        return ResponseEntity.ok(new CommonResponse<>("메시지 발송 완료", messageResponseDto));
    }

    @GetMapping("/members/{memberId}")
    public ResponseEntity<CommonResponse<Page<MessageResponseDto>>> getMessages(@AuthenticationPrincipal UserDetailsImpl userDetails,
                                                                                @PathVariable Long memberId,
                                                                                @RequestParam (defaultValue = "0") int page,
                                                                                @RequestParam (defaultValue = "10") int size) {
        Member member = AuthenticatedMemberUtil.getMember(userDetails);
        Page<MessageResponseDto> messages = messageService.getMessages(member.getId(), page, size);
        return ResponseEntity.ok(new CommonResponse<>("메시지 조회 완료", messages));
    }
}
