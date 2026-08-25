package com.example.newsfeed.notification.dto;

import com.example.newsfeed.notification.entity.Notification;
import lombok.Getter;

import java.time.LocalDateTime;

@Getter
public class NotificationResponseDto {
    private final Long id;
    private final String type;
    private final Long actorId;
    private final String actorName;
    private final Long feedId;
    private final String message;
    private final boolean readStatus;
    private final LocalDateTime createdAt;

    public NotificationResponseDto(Notification n) {
        this.id = n.getId();
        this.type = n.getType() != null ? n.getType().name() : null;
        this.actorId = n.getActorId();
        this.actorName = n.getActorName();
        this.feedId = n.getFeedId();
        this.message = n.getMessage();
        this.readStatus = n.isReadStatus();
        this.createdAt = n.getCreatedAt();
    }
}
