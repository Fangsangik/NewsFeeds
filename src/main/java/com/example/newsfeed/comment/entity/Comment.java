package com.example.newsfeed.comment.entity;

import com.example.newsfeed.feed.entity.Feed;
import com.example.newsfeed.member.entity.Member;
import jakarta.persistence.*;
import lombok.Builder;
import lombok.Getter;

import java.util.ArrayList;
import java.util.List;

import static jakarta.persistence.FetchType.LAZY;
import static jakarta.persistence.GenerationType.IDENTITY;

@Entity
@Getter
public class Comment {

    @Id
    @GeneratedValue(strategy = IDENTITY)
    private Long id;

    private String content;

    @ManyToOne(fetch = LAZY)
    @JoinColumn(name = "feed_id")
    private Feed feed;

    @ManyToOne(fetch = LAZY)
    @JoinColumn(name = "member_id")
    private Member member;

    @ManyToOne(fetch = LAZY)
    @JoinColumn(name = "parent_id")
    private Comment parent;

    @OneToMany(mappedBy = "parent", cascade = CascadeType.ALL)
    private List<Comment> children = new ArrayList<>();

    public Comment() {
    }

    @Builder
    public Comment(String content, Feed feed, Member member, Comment parent, List<Comment> children) {
        this.content = content;
        this.feed = feed;
        this.member = member;
        this.parent = parent;
        this.children = children;
    }

    public void fixComment(String content) {
        this.content = content;
    }

    public void addChild(Comment comment) {
        this.children.add(comment);
        comment.setParent(this);

    }

    public void setParent(Comment parent) {
        this.parent = parent;
    }
}
