package com.example.newsfeed.like.repository;

import com.example.newsfeed.like.entity.Like;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface LikeRepository extends JpaRepository<Like, Long> {


    @Query("select sum(l.likeCount) from Like l where l.feed.id = :feedId")
    Long countByFeedId(@Param("feedId") Long feedId);

    @Query("SELECT l FROM Like l WHERE l.feed.id = :feedId")
    List<Like> findByFeedId(Long feedId);
}
