package com.example.newsfeed.feed.entity;
import com.example.newsfeed.comment.entity.Comment;
import com.example.newsfeed.constants.entity.BaseEntity;
import com.example.newsfeed.like.entity.Like;
import com.example.newsfeed.member.entity.Member;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import jakarta.persistence.*;
import lombok.Builder;
import lombok.Getter;

import java.util.ArrayList;
import java.util.List;

import static jakarta.persistence.FetchType.LAZY;
import static jakarta.persistence.GenerationType.IDENTITY;

@Getter
@Entity
@JsonIgnoreProperties({"hibernateLazyInitializer", "handler"})
public class Feed extends BaseEntity {

    @Id
    @GeneratedValue(strategy = IDENTITY)
    private Long id;

    private String title;
    private String content;

    @ManyToOne(fetch = LAZY)
    @JoinColumn(name = "member_id")
    private Member member;

    @JsonIgnore
    @OneToMany(mappedBy = "feed", cascade = CascadeType.ALL)
    private List<Comment> comments = new ArrayList<>();

    @JsonIgnore
    @OneToMany(mappedBy = "feed", cascade = CascadeType.ALL)
    private List<Like> likes = new ArrayList<>();

    private String image;
    private String address;
    private Double latitude;
    private Double longitude;

    public Feed() {
    }

    @Builder
    public Feed(String title, String content, Member member, List<Comment> comments, List<Like> likes, String image, String address, Double latitude, Double longitude) {
        this.title = title;
        this.content = content;
        this.member = member;
        this.comments = comments;
        this.likes = likes;
        this.image = image;
        this.address = address;
        this.latitude = latitude;
        this.longitude = longitude;
    }

    public void update(String title, String content) {
        if (title != null && !title.trim().isEmpty()) {
            this.title = title;
        }
        if (content != null && !content.trim().isEmpty()) {
            this.content = content;
        }
    }

    public int getLikeCount() {
        return likes.size();
    }
}
