package com.example.newsfeed.message.service;

import com.example.newsfeed.member.entity.Member;
import com.example.newsfeed.message.dto.MessageRequestDto;
import com.example.newsfeed.message.dto.MessageResponseDto;
import org.springframework.data.domain.Page;

public interface MessageService {
    MessageResponseDto sendMessage(Member member, MessageRequestDto messageRequestDto);
    Page<MessageResponseDto> getMessages(Long memberId, int page, int size);
}
