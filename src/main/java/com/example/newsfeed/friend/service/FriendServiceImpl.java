package com.example.newsfeed.friend.service;

import com.example.newsfeed.exception.ErrorCode;
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
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import static com.example.newsfeed.exception.ErrorCode.*;


@Service
public class FriendServiceImpl implements FriendService {

    private final FriendRepository friendRepository;
    private final MemberRepository memberRepository;

    public FriendServiceImpl(FriendRepository friendRepository, MemberRepository memberRepository) {
        this.friendRepository = friendRepository;
        this.memberRepository = memberRepository;
    }

    @Transactional
    public FriendResponseDto addFriend(FriendRequestDto friendRequestDto, Long authenticatedMemberId) {

        if (!authenticatedMemberId.equals(friendRequestDto.getSenderId())) {
            throw new InvalidInputException(NO_AUTHOR);
        }

        Member sender = memberRepository.findById(friendRequestDto.getSenderId())
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

        return FriendResponseDto.toDto(friendRepository.save(friend));
    }

    @Transactional
    public FriendResponseDto acceptFriendRequest(FriendRequestDto friendRequestDto, Long authenticatedMemberId) {
        if (!authenticatedMemberId.equals(friendRequestDto.getReceiverId())) {
            throw new NoAuthorizedException(ErrorCode.NO_AUTHOR);
        }

        Friend friend = friendRepository.findBySenderAndReceiver(friendRequestDto.getSenderId(), friendRequestDto.getReceiverId())
                .orElseThrow(() -> new NoAuthorizedException(NO_AUTHOR_APPROVE));

        // 친구 요청 상태 업데이트
        friend.setStatus(FriendRequestStatus.ACCEPTED);

        friendRepository.save(friend);

        return FriendResponseDto.toDto(friend);
    }

    @Transactional(readOnly = true)
    public Page<FriendSenderResponseDto> findSenderInfo(Long senderId, int page, int size) {

        Pageable pageable = PageRequest.of(page, size);

        // Friend 엔터티를 가져옴
        Page<Friend> friends = friendRepository.findSenderInfo(senderId, pageable);

        return friends.map(friend -> new FriendSenderResponseDto(
                friend.getSender().getName(),
                friend.getSender().getEmail()
        ));
    }

    @Transactional(readOnly = true)
    @Override
    public Page<FriendRequestResponseDto> findReceivedFriendRequests(Long receiverId, int page, int size) {
        Pageable pageable = PageRequest.of(page, size);

        // Friend 엔터티를 가져옴
        Page<Friend> friends = friendRepository.findReceiverInfo(receiverId, pageable);

        return friends.map(friend -> new FriendRequestResponseDto(
                friend.getReceiver().getName(),    // 요청 받은 사람 이름
                friend.getReceiver().getEmail()    // 요청 받은 사람 이메일
        ));
    }

    @Transactional(readOnly = true)
    public Page<FriendListDto> findFriendList(Long memberId, int page, int size) {
        Pageable pageable = PageRequest.of(page, size);

        // Friend 엔터티를 가져옴
        Page<Friend> friends = friendRepository.findFriendList(memberId, pageable);

        // 엔터티를 DTO로 변환
        return friends.map(friend -> new FriendListDto(
                friend.getId(),
                friend.getSender().getName() // 필요한 경우 필드 맞춤 설정
        ));
    }

    @Transactional
    public void deleteFriend(Long friendId) {
        Friend friend = friendRepository.findById(friendId)
                .orElseThrow(() -> new NotFoundException(NOT_FOUND_FRIEND_REQUEST));

        friendRepository.delete(friend);
    }

    private boolean isAlreadyFriend(Member sender, Member receiver) {
        return friendRepository.existsBySenderAndReceiver(sender, receiver)
                || friendRepository.existsBySenderAndReceiver(receiver, sender);
    }
}