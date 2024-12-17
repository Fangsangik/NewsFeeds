package com.example.newsfeed.message.entity;

import com.example.newsfeed.member.entity.Member;
import jakarta.persistence.*;
import lombok.Builder;
import lombok.Getter;

import java.time.LocalDateTime;

@Entity
@Getter
public class Message {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "member_id")
    private Member receiver;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "member_id")
    private Member sender;

    private LocalDateTime timestamp;

    private boolean readStatus;

    @Builder
    public Message(Member receiver, Member sender, LocalDateTime timestamp, boolean readStatus) {
        this.receiver = receiver;
        this.sender = sender;
        this.timestamp = timestamp;
        this.readStatus = readStatus;
    }

    public Message() {

    }
}

