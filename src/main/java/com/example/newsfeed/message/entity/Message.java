package com.example.newsfeed.message.entity;

import com.example.newsfeed.constants.BaseTimeEntity;
import com.example.newsfeed.member.entity.Member;
import jakarta.persistence.*;
import lombok.Builder;
import lombok.Getter;

import java.time.LocalDateTime;

@Entity
@Getter
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

