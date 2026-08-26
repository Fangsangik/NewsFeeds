package com.example.newsfeed.friend.repository;

import com.example.newsfeed.exception.ErrorCode;
import com.example.newsfeed.exception.NotFoundException;
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

    @Query("SELECT f FROM Friend f JOIN FETCH f.sender s where f.sender.id = :senderId AND f.status = 'REQUESTED'")
    Page<Friend> findSenderInfo(@Param("senderId") Long senderId, Pageable pageable);

    @Query("SELECT f FROM Friend f JOIN FETCH f.receiver r where f.receiver.id = :receiverId AND f.status = 'REQUESTED'")
    Page<Friend> findReceiverInfo(@Param("receiverId") Long receiverId, Pageable pageable);

    @Query("SELECT f FROM Friend f WHERE (f.sender.id = :memberId OR f.receiver.id = :memberId) AND f.status = 'ACCEPTED'")
    Page<Friend> findFriendList(@Param("memberId") Long memberId, Pageable pageable);

    @Query("select f from Friend f JOIN FETCH f.sender s where f.sender.id = :senderId and f.receiver.id = :receiverId")
    Optional<Friend> findBySenderAndReceiver(@Param("senderId") Long senderId, @Param("receiverId") Long receiverId);

    // 두 회원 사이의 관계(방향 무관). 중복 행이 있을 수 있어 List로 반환.
    @Query("select f from Friend f where (f.sender.id = :a and f.receiver.id = :b) or (f.sender.id = :b and f.receiver.id = :a)")
    java.util.List<Friend> findBetween(@Param("a") Long a, @Param("b") Long b);

    // 특정 회원의 수락된 친구 수 (상호 모델 → 팔로워=팔로잉)
    @Query("select count(f) from Friend f where (f.sender.id = :m or f.receiver.id = :m) and f.status = com.example.newsfeed.friend.type.FriendRequestStatus.ACCEPTED")
    long countAcceptedFriends(@Param("m") Long m);

    // 특정 회원의 수락된 친구(상대방) id 목록. (CASE는 엔티티 반환 불가 → 스칼라 id만)
    @Query("select case when f.sender.id = :m then f.receiver.id else f.sender.id end from Friend f " +
            "where (f.sender.id = :m or f.receiver.id = :m) and f.status = com.example.newsfeed.friend.type.FriendRequestStatus.ACCEPTED")
    java.util.List<Long> findAcceptedFriendMemberIds(@Param("m") Long m);

    default Friend findByIdOrElseThrow(Long friendId) {
        return findById(friendId).orElseThrow(() -> new NotFoundException(ErrorCode.NOT_FOUND_FRIEND_REQUEST));
    }
}
