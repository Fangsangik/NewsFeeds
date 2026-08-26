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

    @Query("SELECT m FROM Message m " +
            "WHERE (m.sender.id = :me AND m.receiver.id = :peer) " +
            "   OR (m.sender.id = :peer AND m.receiver.id = :me) " +
            "ORDER BY m.createdAt ASC")
    Page<Message> findConversation(Long me, Long peer, Pageable pageable);

    // 나와 상대 사이의 모든 메시지 삭제 (대화 삭제)
    @org.springframework.data.jpa.repository.Modifying(clearAutomatically = true)
    @Query("delete from Message m where (m.sender.id = :me and m.receiver.id = :peer) " +
            "or (m.sender.id = :peer and m.receiver.id = :me)")
    int deleteConversation(@org.springframework.data.repository.query.Param("me") Long me,
                           @org.springframework.data.repository.query.Param("peer") Long peer);
}
