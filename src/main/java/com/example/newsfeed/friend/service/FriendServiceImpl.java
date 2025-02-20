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

    public FriendServiceImpl(FriendRepository friendRepository, MemberRepository memberRepository) {
        this.friendRepository = friendRepository;
        this.memberRepository = memberRepository;
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

        return FriendResponseDto.toDto(friendRepository.save(friend));
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

        return FriendResponseDto.toDto(friend);
    }

    @Override
    public Page<FriendSenderRequestDto> findSenderInfo(Member member, int page, int size) {

        Pageable pageable = PageRequest.of(page, size);

        // Friend 엔터티를 가져옴
        Page<Friend> friends = friendRepository.findSenderInfo(member.getId(), pageable);

        return friends.map(friend -> new FriendSenderRequestDto(
                friend.getSender().getName(),
                friend.getSender().getEmail()
        ));
    }

    @Override
    public Page<FriendRequestResponseDto> findReceivedFriendRequests(Member member, int page, int size) {
        Pageable pageable = PageRequest.of(page, size);

        // Friend 엔터티를 가져옴
        Page<Friend> friends = friendRepository.findReceiverInfo(member.getId(), pageable);

        return friends.map(friend -> new FriendRequestResponseDto(
                friend.getReceiver().getName(),    // 요청 받은 사람 이름
                friend.getReceiver().getEmail()    // 요청 받은 사람 이메일
        ));
    }

    @Override
    public Page<FriendListDto> findFriendList(Member member, int page, int size) {
        Pageable pageable = PageRequest.of(page, size);

        // Friend 엔터티를 가져옴
        Page<Friend> friends = friendRepository.findFriendList(member.getId(), pageable);

        // 엔터티를 DTO로 변환
        return friends.map(friend -> new FriendListDto(
                friend.getId(),
                friend.getSender().getName() // 필요한 경우 필드 맞춤 설정
        ));
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