package com.example.newsfeed.message.service;

import com.example.newsfeed.config.MyWebSocketHandler;
import com.example.newsfeed.member.repository.MemberRepository;
import com.example.newsfeed.message.dto.MessageRequestDto;
import com.example.newsfeed.message.entity.Message;
import com.example.newsfeed.message.repository.MessageRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;

import java.util.Map;

@Slf4j
@Service
public class MessageService {

    private final MessageRepository messageRepository;
    private final MemberRepository memberRepository;
    private final Map<Long, WebSocketSession> userSessions;

    public MessageService(MessageRepository messageRepository, MemberRepository memberRepository, Map<Long, WebSocketSession> userSessions) {
        this.messageRepository = messageRepository;
        this.memberRepository = memberRepository;
        this.userSessions = userSessions;
    }

    @Transactional
    public void sendMessage(MessageRequestDto requestDto) {      // 메시지 생성 및 저장
        Message message = new Message(
                requestDto.getMessage(),
                memberRepository.findById(requestDto.getReceiverId())
                        .orElseThrow(() -> new IllegalArgumentException("받는 사용자가 존재하지 않습니다.")),
                memberRepository.findById(requestDto.getSenderId())
                        .orElseThrow(() -> new IllegalArgumentException("보내는 사용자가 존재하지 않습니다.")),
                false
        );
        messageRepository.save(message);

        WebSocketSession receiverSession = userSessions.get(requestDto.getReceiverId());
        if (receiverSession != null && receiverSession.isOpen()) {
            try {
                receiverSession.sendMessage(new TextMessage(requestDto.getMessage()));
                log.info("메시지가 WebSocket을 통해 전송되었습니다: {}", requestDto.getMessage());
            } catch (Exception e) {
                log.error("WebSocket 메시지 전송 실패: {}", e.getMessage());
            }
        } else {
            log.info("사용자가 오프라인 상태입니다. 메시지는 DB에 저장되었습니다.");
        }
    }

    @Transactional
    public void markAsRead(Long messageId) {
        Message message = messageRepository.findById(messageId)
                .orElseThrow(() -> new IllegalArgumentException("메시지가 존재하지 않습니다."));
        message.setReadStatus(true);
        messageRepository.save(message);
    }

    @Transactional(readOnly = true)
    public Page<MessageRequestDto> getMessages(Long memberId, Pageable pageable) {
        return messageRepository.findAllByReceiverIdAndReadStatusFalse(memberId, pageable);
    }
}
