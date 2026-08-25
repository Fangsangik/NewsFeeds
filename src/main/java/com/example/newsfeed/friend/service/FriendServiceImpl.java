package com.example.newsfeed.friend.service;

import com.example.newsfeed.auth.jwt.service.UserDetailsImpl;
import com.example.newsfeed.exception.InvalidInputException;
import com.example.newsfeed.exception.NoAuthorizedException;
import com.example.newsfeed.exception.NotFoundException;
import com.example.newsfeed.friend.dto.*;
import com.example.newsfeed.friend.entity.Friend;
import com.example.newsfeed.friend.repository.FriendRepository;
import com.example.newsfeed.friend.type.FriendRequestStatus;
import com.example.newsfeed.member.entity.Member;
import com.example.newsfeed.member.repository.MemberRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import static com.example.newsfeed.exception.ErrorCode.*;


@Service
public class FriendServiceImpl implements FriendService {

    private final FriendRepository friendRepository;
    private final MemberRepository memberRepository;
    private final com.example.newsfeed.notification.service.NotificationService notificationService;

    public FriendServiceImpl(FriendRepository friendRepository, MemberRepository memberRepository,
                             com.example.newsfeed.notification.service.NotificationService notificationService) {
        this.friendRepository = friendRepository;
        this.memberRepository = memberRepository;
        this.notificationService = notificationService;
    }

    @Transactional
    @Override
    public FriendResponseDto addFriend(Member member, FriendRequestDto friendRequestDto) {
        Member sender = memberRepository.findById(member.getId())
                .orElseThrow(() -> new NotFoundException(NOT_FOUND_MEMBER));

        Member receiver = memberRepository.findById(friendRequestDto.getReceiverId())
                .orElseThrow(() -> new NotFoundException(NOT_FOUND_FRIEND_REQUEST));

        // 중복된 친구 요청 확인
        if (isAlreadyFriend(sender, receiver)) {
            throw new InvalidInputException(ALREADY_FRIEND);
        }

        Friend friend = Friend.builder()
                .sender(sender)
                .receiver(receiver)
                .status(FriendRequestStatus.REQUESTED)
                .build();
        FriendResponseDto dto = FriendResponseDto.toDto(friendRepository.save(friend));

        // 요청 받은 사람에게 친구요청 알림
        notificationService.notify(receiver.getId(),
                com.example.newsfeed.notification.entity.Notification.Type.FRIEND_REQUEST,
                sender.getId(), sender.getName(), null,
                sender.getName() + "님이 친구 요청을 보냈습니다.");
        return dto;
    }

    @Transactional
    @Override
    public FriendResponseDto acceptFriendRequest(Member member, FriendAcceptRequestDto friendAcceptRequestDto) {

        // 친구 요청 조회 (receiver와 sender 기준으로)
        Friend friend = friendRepository.findBySenderAndReceiver(
                        friendAcceptRequestDto.getSenderId(), member.getId())
                .orElseThrow(() -> new NoAuthorizedException(NO_AUTHOR_APPROVE));

        // 친구 요청 상태 업데이트
        friend.setStatus(FriendRequestStatus.ACCEPTED);

        friendRepository.save(friend);

        // 요청 보낸 사람에게 수락 알림 (수락자 = receiver = 나)
        notificationService.notify(friend.getSender().getId(),
                com.example.newsfeed.notification.entity.Notification.Type.FRIEND_ACCEPT,
                friend.getReceiver().getId(), friend.getReceiver().getName(), null,
                friend.getReceiver().getName() + "님이 친구 요청을 수락했습니다.");

        return FriendResponseDto.toDto(friend);
    }

    @Override
    public Page<FriendSenderRequestDto> findSenderInfo(Member member, int page, int size) {

        Pageable pageable = PageRequest.of(page, size);

        // Friend 엔터티를 가져옴
        Page<Friend> friends = friendRepository.findSenderInfo(member.getId(), pageable);

        // 보낸 요청 목록에는 '요청을 받은 사람(receiver)'을 보여줘야 한다.
        return friends.map(friend -> new FriendSenderRequestDto(
                friend.getReceiver().getEmail(),
                friend.getReceiver().getName()
        ));
    }

    @Override
    public Page<FriendRequestResponseDto> findReceivedFriendRequests(Member member, int page, int size) {
        Pageable pageable = PageRequest.of(page, size);

        // Friend 엔터티를 가져옴
        Page<Friend> friends = friendRepository.findReceiverInfo(member.getId(), pageable);

        // 받은 요청 목록에는 '요청을 보낸 사람(sender)'을 보여줘야 한다. (email, name 슬롯 순서 주의)
        return friends.map(friend -> new FriendRequestResponseDto(
                friend.getSender().getEmail(),     // 요청 보낸 사람 이메일
                friend.getSender().getName()       // 요청 보낸 사람 이름
        ));
    }

    @Override
    public Page<FriendListDto> findFriendList(Member member, int page, int size) {
        Pageable pageable = PageRequest.of(page, size);

        // Friend 엔터티를 가져옴
        Page<Friend> friends = friendRepository.findFriendList(member.getId(), pageable);

        // 엔터티를 DTO로 변환 — 로그인 회원 기준 '상대방'의 memberId + 이름을 반환한다.
        // (기존엔 Friend row PK와 항상 sender 이름을 반환해 DM이 엉뚱한 회원에게 가고 상대 이름이 틀렸다.)
        Long meId = member.getId();
        return friends.map(friend -> {
            var peer = friend.getSender().getId().equals(meId) ? friend.getReceiver() : friend.getSender();
            return new FriendListDto(peer.getId(), peer.getName());
        });
    }

    @Transactional
    @Override
    public void deleteFriend(Member member, Long friendId) {
        if (!memberRepository.existsById(member.getId())) {
            throw new NotFoundException(NOT_FOUND_MEMBER);
        }

        Friend findFriend = friendRepository.findByIdOrElseThrow(friendId);

        friendRepository.delete(findFriend);
    }

    private boolean isAlreadyFriend(Member sender, Member receiver) {
        return friendRepository.existsBySenderAndReceiver(sender, receiver)
                || friendRepository.existsBySenderAndReceiver(receiver, sender);
    }
}