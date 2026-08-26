package com.example.newsfeed.block.entity;

import com.example.newsfeed.member.entity.Member;
import jakarta.persistence.*;
import lombok.Builder;
import lombok.Getter;

import static jakarta.persistence.FetchType.LAZY;
import static jakarta.persistence.GenerationType.IDENTITY;

/** 차단: blocker가 blocked를 차단. (blocker당 blocked 1행) */
@Entity
@Getter
@Table(name = "blocks",
        uniqueConstraints = @UniqueConstraint(columnNames = {"blocker_id", "blocked_id"}))
public class Block {

    @Id
    @GeneratedValue(strategy = IDENTITY)
    private Long id;

    @ManyToOne(fetch = LAZY)
    @JoinColumn(name = "blocker_id")
    private Member blocker;

    @ManyToOne(fetch = LAZY)
    @JoinColumn(name = "blocked_id")
    private Member blocked;

    public Block() {}

    @Builder
    public Block(Member blocker, Member blocked) {
        this.blocker = blocker;
        this.blocked = blocked;
    }
}
