package com.example.newsfeed.comment.repository;

import com.example.newsfeed.comment.entity.Comment;
import com.example.newsfeed.exception.NotFoundException;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

import static com.example.newsfeed.exception.ErrorCode.NOT_FOUND_COMMENT;

public interface CommentRepository extends JpaRepository<Comment, Long> {

    @Query("SELECT c FROM Comment c LEFT JOIN FETCH c.children WHERE c.feed.id = :feedId")
    List<Comment> findByFeedId(@Param("feedId") Long feedId);

    @Query("SELECT c FROM Comment c LEFT JOIN FETCH c.children WHERE c.id = :commentId")
    Optional<Comment> findByIdWithReplies(@Param("commentId") Long commentId);

    @Query("SELECT c FROM Comment c " +
            "JOIN fetch c.feed f JOIN fetch c.member m WHERE c.id = :commentId")
    Optional<Comment> findByIdWithFeedAndMember(@Param("commentId") Long commentId);

    default Comment findByIdOrElseThrow(Long commentId) {
        return findByIdWithFeedAndMember(commentId)
                .orElseThrow(() -> new NotFoundException(NOT_FOUND_COMMENT));
    }
}
