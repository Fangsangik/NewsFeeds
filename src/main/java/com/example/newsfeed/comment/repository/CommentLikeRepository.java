package com.example.newsfeed.comment.repository;

import com.example.newsfeed.comment.entity.CommentLike;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface CommentLikeRepository extends JpaRepository<CommentLike, Long> {

    boolean existsByComment_IdAndMember_Id(Long commentId, Long memberId);

    long countByComment_Id(Long commentId);

    @Modifying(clearAutomatically = true)
    @Query("delete from CommentLike cl where cl.comment.id = :commentId and cl.member.id = :memberId")
    int deleteByCommentAndMember(@Param("commentId") Long commentId, @Param("memberId") Long memberId);

    // 여러 댓글의 좋아요 수를 한 번에 (commentId, count)
    @Query("select cl.comment.id, count(cl) from CommentLike cl where cl.comment.id in :ids group by cl.comment.id")
    List<Object[]> countByCommentIds(@Param("ids") List<Long> ids);

    // 내가 좋아요한 댓글 id들
    @Query("select cl.comment.id from CommentLike cl where cl.comment.id in :ids and cl.member.id = :memberId")
    List<Long> likedCommentIds(@Param("ids") List<Long> ids, @Param("memberId") Long memberId);
}
