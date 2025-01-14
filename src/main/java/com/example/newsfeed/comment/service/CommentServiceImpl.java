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
public class CommentServiceImpl implements CommentService{

    private final CommentRepository commentRepository;
    private final FeedRepository feedRepository;
    private final MemberRepository memberRepository;

    public CommentServiceImpl(CommentRepository commentRepository, FeedRepository feedRepository, MemberRepository memberRepository) {
        this.commentRepository = commentRepository;
        this.feedRepository = feedRepository;
        this.memberRepository = memberRepository;
    }

    @Transactional
    @Override
    public CommentResponseDto createComment(CommentRequestDto commentRequestDto, Long authenticatedMemberId) {

        validateMemberId(!authenticatedMemberId.equals(commentRequestDto.getMemberId()), ErrorCode.NO_AUTHOR);

        Member member = memberRepository.findById(commentRequestDto.getMemberId())
                .orElseThrow(() -> new NotFoundException(NOT_FOUND_MEMBER));

        Feed feed = feedRepository.findById(commentRequestDto.getFeedId())
                .orElseThrow(() -> new NotFoundException(ErrorCode.NOT_FOUND_NEWSFEED));

        // 부모 댓글 조회
        Comment parentComment = null;
        if (commentRequestDto.getParentId() != null) {
            parentComment = commentRepository.findById(commentRequestDto.getParentId())
                    .orElseThrow(() -> new NotFoundException(ErrorCode.NOT_FOUND_COMMENT));
        }

        validateMemberId(parentComment != null && !parentComment.getFeed().equals(feed), ErrorCode.NOT_FOUND_PARENT_COMMENT);

        // Comment 생성
        Comment comment = CommentRequestDto.toEntity(member, feed, commentRequestDto, parentComment);

        // 저장
        Comment savedComment = commentRepository.save(comment);

        // ResponseDto 반환
        return CommentResponseDto.toDto(savedComment);
    }

    @Transactional
    @Override
    public CommentResponseDto updateComment(Long commentId, CommentRequestDto commentRequestDto, Long authenticatedMemberId) {

        validateMemberId(!authenticatedMemberId.equals(commentRequestDto.getMemberId()), ErrorCode.NO_AUTHOR);

        // 댓글 조회
        Comment comment = commentRepository.findById(commentId)
                .orElseThrow(() -> new NotFoundException(ErrorCode.NOT_FOUND_COMMENT));

        // 대댓글인지 확인 (필요 시 추가 검증)
        validateMemberId(commentRequestDto.isChildComment() && comment.getParent() == null, ErrorCode.NOT_FOUND_COMMENT);

        // 댓글 내용 수정
        comment.fixComment(commentRequestDto.getContent());

        // 수정된 댓글 저장
        Comment updatedComment = commentRepository.save(comment);

        return CommentResponseDto.toDto(updatedComment);
    }

    @Transactional
    @Override
    public CommentChildResponseDto createChildComment(CommentRequestDto requestDto, Long authenticatedMemberId) {

        validateMemberId(!authenticatedMemberId.equals(requestDto.getMemberId()), ErrorCode.NO_AUTHOR);

        Comment parentComment = commentRepository.findByIdWithFeedAndMember(requestDto.getParentId())
                .orElseThrow(() -> new NotFoundException(ErrorCode.NOT_FOUND_PARENT_COMMENT));

        // 사용자 조회
        Member member = memberRepository.findById(requestDto.getMemberId())
                .orElseThrow(() -> new NotFoundException(NOT_FOUND_MEMBER));

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
        return comments.stream()
                .filter(comment -> comment.getParent() == null) // 부모가 없는 댓글만 필터링
                .map(CommentResponseDto::toDto)
                .toList();
    }

    @Transactional
    @Override
    public void deleteComment(Long commentId) {
        Comment comment = commentRepository.findById(commentId)
                .orElseThrow(() -> new NotFoundException(NOT_FOUND_COMMENT));

        commentRepository.delete(comment);
    }

    private static void validateMemberId(boolean authenticatedMemberId, ErrorCode noAuthor) {
        if (authenticatedMemberId) {
            throw new NotFoundException(noAuthor);
        }
    }
}
