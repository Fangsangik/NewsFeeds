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
        // 메시지 저장에는 sender/receiver의 FK(id)만 있으면 된다. getReferenceById는 실제
        // SELECT 없이 프록시만 반환하므로 메시지당 member SELECT 2건을 제거한다.
        // (senderId는 인증 주체라 항상 유효. 존재하지 않는 receiverId는 INSERT 시 FK 제약으로 걸린다.)
        Member sender = memberRepository.getReferenceById(senderId);
        Member receiver = memberRepository.getReferenceById(receiverId);

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
        if (message.isReadStatus()) return; // 이미 읽음이면 중복 푸시 방지
        message.setReadStatus(true);
        // 발신자에게 '읽음' 실시간 푸시 (접속 중이면 즉시 '읽음' 표시)
        try {
            Long senderId = message.getSender().getId();
            Long peerId = message.getReceiver().getId(); // 읽은 사람(= 발신자 입장에선 대화 상대)
            messagingTemplate.convertAndSendToUser(String.valueOf(senderId), "/queue/read",
                    java.util.Map.of("messageId", messageId, "peerId", peerId));
        } catch (Exception e) {
            log.warn("읽음 푸시 실패(무시): {}", e.getMessage());
        }
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
