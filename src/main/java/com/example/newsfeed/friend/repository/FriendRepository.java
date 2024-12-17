package com.example.newsfeed.friend.repository;

import com.example.newsfeed.friend.entity.Friend;
import com.example.newsfeed.member.entity.Member;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

public interface FriendRepository extends JpaRepository<Friend, Long> {

    boolean existsBySenderAndReceiver(Member sender, Member receiver);

    @Query("SELECT f FROM Friend f WHERE f.sender.id = :senderId AND f.status = 'REQUESTED'")
    Page<Friend> findSenderInfo(@Param("senderId") Long senderId, Pageable pageable);

    @Query("SELECT f FROM Friend f WHERE f.receiver.id = :receiverId AND f.status = 'REQUESTED'")
    Page<Friend> findReceiverInfo(@Param("receiverId") Long receiverId, Pageable pageable);

    @Query("SELECT f FROM Friend f WHERE (f.sender.id = :memberId OR f.receiver.id = :memberId) AND f.status = 'ACCEPTED'")
    Page<Friend> findFriendList(@Param("memberId") Long memberId, Pageable pageable);

    @Query("select f from Friend f where f.sender.id = :senderId and f.receiver.id = :receiverId")
    Optional<Friend> findBySenderAndReceiver(@Param("senderId") Long senderId, @Param("receiverId") Long receiverId);

}
