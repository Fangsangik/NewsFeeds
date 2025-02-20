package com.example.newsfeed.message.repository;

import com.example.newsfeed.message.entity.Message;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

public interface MessageRepository extends JpaRepository<Message, Long> {

    Page<Message> findByMemberId(Long memberId, Pageable pageable);
}
