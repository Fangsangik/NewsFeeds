package com.example.newsfeed.comment.service;

import com.example.newsfeed.comment.dto.CommentChildResponseDto;
import com.example.newsfeed.comment.dto.CommentRequestDto;
import com.example.newsfeed.comment.dto.CommentResponseDto;
import com.example.newsfeed.comment.entity.Comment;
import com.example.newsfeed.comment.repository.CommentRepository;
import com.example.newsfeed.exception.ErrorCode;
import com.example.newsfeed.exception.NotFoundException;
import com.example.newsfeed.feed.entity.Feed;
import com.example.newsfeed.feed.repository.FeedRepository;
import com.example.newsfeed.member.entity.Member;
import com.example.newsfeed.member.repository.MemberRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

import static com.example.newsfeed.exception.ErrorCode.NOT_FOUND_COMMENT;
import static com.example.newsfeed.exception.ErrorCode.NOT_FOUND_MEMBER;

@Service
public class CommentServiceImpl implements CommentService {

    private final CommentRepository commentRepository;
    private final FeedRepository feedRepository;
    private final MemberRepository memberRepository;
    private final com.example.newsfeed.notification.service.NotificationService notificationService;
    private final com.example.newsfeed.comment.repository.CommentLikeRepository commentLikeRepository;

    public CommentServiceImpl(CommentRepository commentRepository, FeedRepository feedRepository,
                              MemberRepository memberRepository,
                              com.example.newsfeed.notification.service.NotificationService notificationService,
                              com.example.newsfeed.comment.repository.CommentLikeRepository commentLikeRepository) {
        this.commentRepository = commentRepository;
        this.feedRepository = feedRepository;
        this.memberRepository = memberRepository;
        this.notificationService = notificationService;
        this.commentLikeRepository = commentLikeRepository;
    }

    @Transactional
    @Override
    public CommentResponseDto createComment(CommentRequestDto commentRequestDto, Member member) {

        memberRepository.findByIdOrElseThrow(member.getId());

        Feed feed = feedRepository.findById(commentRequestDto.getFeedId())
                .orElseThrow(() -> new NotFoundException(ErrorCode.NOT_FOUND_NEWSFEED));

        // 부모 댓글 조회
        Comment parentComment = null;
        if (commentRequestDto.getParentId() != null) {
            parentComment = commentRepository.findByIdOrElseThrow(commentRequestDto.getParentId());
        }

        // Comment 생성
        Comment comment = CommentRequestDto.toEntity(member, feed, commentRequestDto, parentComment);

        // 저장
        Comment savedComment = commentRepository.save(comment);

        // 게시물 작성자에게 댓글 알림 (본인 글은 무시)
        String actorName = memberRepository.findById(member.getId()).map(m -> m.getName()).orElse("사용자");
        notificationService.notify(feed.getMember().getId(),
                com.example.newsfeed.notification.entity.Notification.Type.COMMENT,
                member.getId(), actorName, feed.getId(),
                actorName + "님이 회원님의 게시물에 댓글을 남겼습니다.");

        // ResponseDto 반환
        return CommentResponseDto.toDto(savedComment);
    }

    @Transactional
    @Override
    public CommentResponseDto updateComment(Long commentId, CommentRequestDto commentRequestDto, Member member) {

        memberRepository.findByIdOrElseThrow(member.getId());

        // 댓글 조회
        Comment comment = commentRepository.findByIdOrElseThrow(commentId);

        // 대댓글인지 확인 (필요 시 추가 검증)
        if (commentRequestDto.isChildComment() && comment.getParent() == null) {
            throw new NotFoundException(ErrorCode.NOT_FOUND_COMMENT);
        }

        // 댓글 내용 수정
        comment.fixComment(commentRequestDto.getContent());

        // 수정된 댓글 저장
        Comment updatedComment = commentRepository.save(comment);

        return CommentResponseDto.toDto(updatedComment);
    }

    @Transactional
    @Override
    public CommentChildResponseDto createChildComment(CommentRequestDto requestDto, Member member) {

        Comment parentComment = commentRepository.findByIdWithFeedAndMember(requestDto.getParentId())
                .orElseThrow(() -> new NotFoundException(ErrorCode.NOT_FOUND_PARENT_COMMENT));

        // 사용자 조회
        memberRepository.findByIdOrElseThrow(member.getId());

        // 대댓글 생성시 검증 필요?

        Comment childComment = Comment.builder()
                .content(requestDto.getContent())
                .member(member)
                .feed(parentComment.getFeed())
                .build();

        parentComment.addChild(childComment);

        Comment savedComment = commentRepository.save(childComment);

        return CommentChildResponseDto.toDto(savedComment);
    }

    @Transactional(readOnly = true)
    @Override
    public CommentResponseDto getComment(Long commentId) {
        Comment comment = commentRepository.findByIdWithReplies(commentId)
                .orElseThrow(() -> new NotFoundException(NOT_FOUND_COMMENT));

        return CommentResponseDto.toDto(comment);
    }

    @Transactional(readOnly = true)
    public List<CommentResponseDto> getCommentsByFeedId(Long feedId) {
        List<Comment> comments = commentRepository.findByFeedId(feedId);

        // 부모 댓글만 필터링하고 대댓글 계층 포함
        List<CommentResponseDto> dtos = comments.stream()
                .filter(comment -> comment.getParent() == null)
                .map(CommentResponseDto::toDto)
                .toList();

        fillLikeInfo(dtos);
        return dtos;
    }

    /** 댓글(+대댓글)에 좋아요 수·내 좋아요 여부를 채운다. 비로그인이면 likedByMe=false. */
    private void fillLikeInfo(List<CommentResponseDto> dtos) {
        List<Long> ids = new java.util.ArrayList<>();
        collectIds(dtos, ids);
        if (ids.isEmpty()) return;

        java.util.Map<Long, Long> countMap = new java.util.HashMap<>();
        for (Object[] row : commentLikeRepository.countByCommentIds(ids)) {
            countMap.put((Long) row[0], (Long) row[1]);
        }
        Long meId = com.example.newsfeed.util.AuthenticatedMemberUtil.getAuthenticatedMemberIdOrNull();
        java.util.Set<Long> likedIds = meId == null ? java.util.Set.of()
                : new java.util.HashSet<>(commentLikeRepository.likedCommentIds(ids, meId));

        applyLikeInfo(dtos, countMap, likedIds);
    }

    private void collectIds(List<CommentResponseDto> dtos, List<Long> out) {
        for (CommentResponseDto d : dtos) {
            out.add(d.getCommentId());
            if (d.getChildComments() != null) collectIds(d.getChildComments(), out);
        }
    }

    private void applyLikeInfo(List<CommentResponseDto> dtos, java.util.Map<Long, Long> countMap, java.util.Set<Long> likedIds) {
        for (CommentResponseDto d : dtos) {
            d.setLikeCount(countMap.getOrDefault(d.getCommentId(), 0L));
            d.setLikedByMe(likedIds.contains(d.getCommentId()));
            if (d.getChildComments() != null) applyLikeInfo(d.getChildComments(), countMap, likedIds);
        }
    }

    @Transactional
    @Override
    public void deleteComment(Member member, Long commentId) {
        // 존재 체크가 반전돼 있어(회원이 있으면 예외) 모든 삭제가 404였다 → 부정 조건으로 수정.
        if (!memberRepository.existsById(member.getId())) {
            throw new NotFoundException(NOT_FOUND_MEMBER);
        }

        Comment comment = commentRepository.findById(commentId)
                .orElseThrow(() -> new NotFoundException(NOT_FOUND_COMMENT));

        commentRepository.delete(comment);
    }
}
