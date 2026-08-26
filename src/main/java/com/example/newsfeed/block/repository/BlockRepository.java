package com.example.newsfeed.block.repository;

import com.example.newsfeed.block.entity.Block;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface BlockRepository extends JpaRepository<Block, Long> {

    boolean existsByBlocker_IdAndBlocked_Id(Long blockerId, Long blockedId);

    @Modifying(clearAutomatically = true)
    @Query("delete from Block b where b.blocker.id = :blockerId and b.blocked.id = :blockedId")
    int deleteByBlockerAndBlocked(@Param("blockerId") Long blockerId, @Param("blockedId") Long blockedId);

    // 내가 차단한 회원 id
    @Query("select b.blocked.id from Block b where b.blocker.id = :me")
    List<Long> blockedIds(@Param("me") Long me);

    // 내가 차단한 회원 목록(카드용)
    @Query("select b.blocked.id from Block b where b.blocker.id = :me order by b.id desc")
    List<Long> blockedIdsOrdered(@Param("me") Long me);
}
