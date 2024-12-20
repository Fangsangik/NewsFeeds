package com.example.newsfeed.message.repository;

import com.example.newsfeed.message.dto.MessageRequestDto;
import com.example.newsfeed.message.entity.Message;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;

public interface MessageRepository extends JpaRepository<Message, Long> {
    List<Message> findByReceiverIdAndReadStatusFalse(Long memberId);

    @Query("SELECT new com.example.newsfeed.message.dto.MessageRequestDto(m.sender.id, m.receiver.id, m.message)" +
            " FROM Message m WHERE m.receiver.id = :memberId AND m.readStatus = false")
    Page<MessageRequestDto> findAllByReceiverIdAndReadStatusFalse(Long memberId, Pageable pageable);

    @Query("SELECT new com.example.newsfeed.message.dto.MessageRequestDto(m.sender.id, m.receiver.id, m.message)" +
            " FROM Message m WHERE m.receiver.id = :memberId")
    Page<MessageRequestDto> findAllByReceiverId(Long memberId, Pageable pageable);
}
