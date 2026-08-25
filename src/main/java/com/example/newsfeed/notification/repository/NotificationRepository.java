package com.example.newsfeed.notification.repository;

import com.example.newsfeed.notification.entity.Notification;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface NotificationRepository extends JpaRepository<Notification, Long> {

    Page<Notification> findByRecipient_IdOrderByIdDesc(Long recipientId, Pageable pageable);

    long countByRecipient_IdAndReadStatusFalse(Long recipientId);

    @Modifying(clearAutomatically = true)
    @Query("update Notification n set n.readStatus = true where n.recipient.id = :recipientId and n.readStatus = false")
    int markAllRead(@Param("recipientId") Long recipientId);
}
