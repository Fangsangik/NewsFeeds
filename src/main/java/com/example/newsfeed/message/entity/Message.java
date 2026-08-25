package com.example.newsfeed.message.entity;

import com.example.newsfeed.constants.entity.BaseTimeEntity;
import com.example.newsfeed.member.entity.Member;
import jakarta.persistence.*;
import lombok.Builder;
import lombok.Getter;

import java.time.LocalDateTime;

@Entity
@Getter
@Table(indexes = {
        // 대화 조회: WHERE (sender=me AND receiver=peer) OR (sender=peer AND receiver=me) ORDER BY created_at
        // OR의 양방향을 각각 커버 + created_at 정렬을 인덱스로 흡수(filesort 제거).
        @Index(name = "idx_msg_conv", columnList = "sender_id, receiver_id, created_at"),
        @Index(name = "idx_msg_conv_rev", columnList = "receiver_id, sender_id, created_at"),
        // 안 읽은 메시지: WHERE receiver_id = ? AND read_status = false
        @Index(name = "idx_msg_unread", columnList = "receiver_id, read_status")
})
public class Message extends BaseTimeEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String message;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "receiver_id")
    private Member receiver;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "sender_id")
    private Member sender;

    private boolean readStatus;

    @Builder
    public Message(String message, Member receiver, Member sender,  boolean readStatus) {
        this.message = message;
        this.receiver = receiver;
        this.sender = sender;
        this.readStatus = readStatus;
    }

    public Message() {

    }

    public void setReadStatus(boolean readStatus) {
        this.readStatus = readStatus;
    }
}

