package com.example.newsfeed.message.service;

import com.example.newsfeed.exception.NotFoundException;
import com.example.newsfeed.member.entity.Member;
import com.example.newsfeed.member.repository.MemberRepository;
import com.example.newsfeed.message.dto.MessageRequestDto;
import com.example.newsfeed.message.dto.MessageResponseDto;
import com.example.newsfeed.message.entity.Message;
import com.example.newsfeed.message.repository.MessageRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import static com.example.newsfeed.exception.ErrorCode.*;

@Slf4j
@Service
public class MessageService {

    private final MessageRepository messageRepository;
    private final MemberRepository memberRepository;
    private final SimpMessagingTemplate messagingTemplate;

    public MessageService(MessageRepository messageRepository,
                          MemberRepository memberRepository,
                          SimpMessagingTemplate messagingTemplate) {
        this.messageRepository = messageRepository;
        this.memberRepository = memberRepository;
        this.messagingTemplate = messagingTemplate;
    }

    @Transactional
    public MessageResponseDto sendMessage(Long senderId, Long receiverId, String content) {
        Member sender = memberRepository.findById(senderId)
                .orElseThrow(() -> new NotFoundException(NOT_FOUND_MEMBER));
        Member receiver = memberRepository.findById(receiverId)
                .orElseThrow(() -> new NotFoundException(NOT_FOUND_MEMBER));

        Message saved = messageRepository.save(new Message(content, receiver, sender, false));
        MessageResponseDto dto = MessageResponseDto.from(saved);

        // Push to receiver via STOMP user-destination. If they aren't subscribed
        // it silently no-ops and the message is still safely in the DB.
        try {
            messagingTemplate.convertAndSendToUser(
                    String.valueOf(receiverId), "/queue/messages", dto);
        } catch (Exception e) {
            log.warn("STOMP push failed (offline?): {}", e.getMessage());
        }
        return dto;
    }

    @Transactional
    public void markAsRead(Long messageId) {
        Message message = messageRepository.findById(messageId)
                .orElseThrow(() -> new NotFoundException(NOT_FOUND_MESSAGE));
        message.setReadStatus(true);
    }

    @Transactional(readOnly = true)
    public Page<MessageRequestDto> getUnread(Long memberId, Pageable pageable) {
        return messageRepository.findAllByReceiverIdAndReadStatusFalse(memberId, pageable);
    }

    @Transactional(readOnly = true)
    public Page<MessageRequestDto> getInbox(Long memberId, Pageable pageable) {
        return messageRepository.findAllByReceiverId(memberId, pageable);
    }

    @Transactional(readOnly = true)
    public Page<MessageResponseDto> getConversation(Long meId, Long peerId, Pageable pageable) {
        return messageRepository.findConversation(meId, peerId, pageable)
                .map(MessageResponseDto::from);
    }
}
