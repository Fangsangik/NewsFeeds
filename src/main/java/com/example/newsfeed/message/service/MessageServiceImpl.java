package com.example.newsfeed.message.service;

import com.example.newsfeed.member.entity.Member;
import com.example.newsfeed.member.repository.MemberRepository;
import com.example.newsfeed.message.dto.MessageRequestDto;
import com.example.newsfeed.message.dto.MessageResponseDto;
import com.example.newsfeed.message.entity.Message;
import com.example.newsfeed.message.repository.MessageRepository;
import com.example.newsfeed.message.socket.MessagePublisher;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
public class MessageServiceImpl implements MessageService {

    private final MessageRepository messageRepository;
    private final MemberRepository memberRepository;
    private final MessagePublisher messagePublisher;

    public MessageServiceImpl(MessageRepository messageRepository, MemberRepository memberRepository, MessagePublisher messagePublisher) {
        this.messageRepository = messageRepository;
        this.memberRepository = memberRepository;
        this.messagePublisher = messagePublisher;
    }

    @Transactional
    @Override
    public MessageResponseDto sendMessage(Member member, MessageRequestDto messageRequestDto) {
        Member findMember = memberRepository.findByIdOrElseThrow(member.getId());

        Message message = Message.builder()
                .member(findMember)
                .content(messageRequestDto.getContent())
                .build();

        Message savedMessage = messageRepository.save(message);
        messagePublisher.publish("chat", savedMessage.getContent());
        log.info("✉️ 메시지 발송: {}", savedMessage.getContent());

        return MessageResponseDto.toDto(savedMessage);
    }

    @Override
    public Page<MessageResponseDto> getMessages(Long memberId, int page, int size) {
        Pageable pageable = PageRequest.of(page, size);
        Member findMember = memberRepository.findByIdOrElseThrow(memberId);

        return messageRepository.findByMemberId(findMember.getId(), pageable)
                .map(MessageResponseDto::toDto);
    }
}
