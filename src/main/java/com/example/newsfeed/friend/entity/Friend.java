package com.example.newsfeed.friend.entity;

import com.example.newsfeed.friend.type.FriendRequestStatus;
import com.example.newsfeed.member.entity.Member;
import jakarta.persistence.*;
import lombok.Builder;
import lombok.Getter;

@Entity
@Getter
public class Friend {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "sender_id")
    private Member sender;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "receiver_id")
    private Member receiver;

    @Enumerated(EnumType.STRING)
    private FriendRequestStatus status;

    public Friend() {
    }

    @Builder
    public Friend(Member sender, Member receiver, FriendRequestStatus status) {
        this.sender = sender;
        this.receiver = receiver;
        this.status = status;
    }

    public void acceptMemberSession(Long memberId) {
        if (!this.receiver.getId().equals(memberId)) {
            throw new IllegalArgumentException("수락 권한이 없습니다.");
        }

        if (this.status != FriendRequestStatus.REQUESTED) {
            throw new IllegalStateException("이미 처리된 요청입니다.");
        }

        this.status = FriendRequestStatus.ACCEPTED;
    }

    public void setStatus(FriendRequestStatus status) {
        this.status = status;
    }

}
