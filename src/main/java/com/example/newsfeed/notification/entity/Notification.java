package com.example.newsfeed.notification.entity;

import com.example.newsfeed.member.entity.Member;
import jakarta.persistence.*;
import lombok.Builder;
import lombok.Getter;

import java.time.LocalDateTime;

import static jakarta.persistence.FetchType.LAZY;
import static jakarta.persistence.GenerationType.IDENTITY;

/** 알림. 특정 회원(recipient)에게 도착하는 이벤트(좋아요/댓글/친구요청/수락). */
@Entity
@Getter
@Table(name = "notifications", indexes = @Index(name = "idx_noti_recipient", columnList = "recipient_id, id"))
public class Notification {

    public enum Type { LIKE, COMMENT, FRIEND_REQUEST, FRIEND_ACCEPT }

    @Id
    @GeneratedValue(strategy = IDENTITY)
    private Long id;

    @ManyToOne(fetch = LAZY)
    @JoinColumn(name = "recipient_id")
    private Member recipient;

    @Enumerated(EnumType.STRING)
    private Type type;

    private Long actorId;      // 행위자(누른/단/요청한 사람)
    private String actorName;
    private Long feedId;       // 관련 피드(친구 알림은 null)
    private String message;

    private boolean readStatus;
    private LocalDateTime createdAt;

    public Notification() {}

    @Builder
    public Notification(Member recipient, Type type, Long actorId, String actorName, Long feedId, String message, LocalDateTime createdAt) {
        this.recipient = recipient;
        this.type = type;
        this.actorId = actorId;
        this.actorName = actorName;
        this.feedId = feedId;
        this.message = message;
        this.readStatus = false;
        this.createdAt = createdAt;
    }

    public void markRead() { this.readStatus = true; }
}
