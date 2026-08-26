package com.example.newsfeed.notification.service;

import com.example.newsfeed.member.repository.MemberRepository;
import com.example.newsfeed.notification.dto.NotificationResponseDto;
import com.example.newsfeed.notification.entity.Notification;
import com.example.newsfeed.notification.repository.NotificationRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

@Slf4j
@Service
public class NotificationService {

    private final NotificationRepository notificationRepository;
    private final MemberRepository memberRepository;
    private final SimpMessagingTemplate messagingTemplate;

    public NotificationService(NotificationRepository notificationRepository, MemberRepository memberRepository,
                               SimpMessagingTemplate messagingTemplate) {
        this.notificationRepository = notificationRepository;
        this.memberRepository = memberRepository;
        this.messagingTemplate = messagingTemplate;
    }

    /**
     * 알림 생성. 본인에게 가는 알림(자기 글 좋아요 등)은 만들지 않는다.
     * 알림 실패가 본질 동작(좋아요/댓글)을 깨지 않도록 예외를 삼킨다.
     * 저장 후 수신자에게 STOMP로 실시간 푸시한다.
     */
    @Transactional
    public void notify(Long recipientId, Notification.Type type, Long actorId, String actorName, Long feedId, String message) {
        try {
            if (recipientId == null || recipientId.equals(actorId)) return;
            Notification saved = notificationRepository.save(Notification.builder()
                    .recipient(memberRepository.getReferenceById(recipientId))
                    .type(type)
                    .actorId(actorId)
                    .actorName(actorName)
                    .feedId(feedId)
                    .message(message)
                    .createdAt(LocalDateTime.now())
                    .build());
            // 수신자가 접속 중이면 실시간 도착. 미접속이면 no-op(다음 조회 시 뱃지 반영).
            try {
                messagingTemplate.convertAndSendToUser(String.valueOf(recipientId),
                        "/queue/notifications", new NotificationResponseDto(saved));
            } catch (Exception pushErr) {
                log.warn("알림 푸시 실패(무시): {}", pushErr.getMessage());
            }
        } catch (Exception e) {
            log.warn("알림 생성 실패(무시): {}", e.getMessage());
        }
    }

    @Transactional(readOnly = true)
    public Page<NotificationResponseDto> list(Long recipientId, int page, int size) {
        return notificationRepository.findByRecipient_IdOrderByIdDesc(recipientId, PageRequest.of(page, size))
                .map(NotificationResponseDto::new);
    }

    @Transactional(readOnly = true)
    public long unreadCount(Long recipientId) {
        return notificationRepository.countByRecipient_IdAndReadStatusFalse(recipientId);
    }

    @Transactional
    public void markAllRead(Long recipientId) {
        notificationRepository.markAllRead(recipientId);
    }
}
