package com.example.newsfeed.block.controller;

import com.example.newsfeed.block.entity.Block;
import com.example.newsfeed.block.repository.BlockRepository;
import com.example.newsfeed.constants.response.CommonResponse;
import com.example.newsfeed.friend.dto.FriendMemberDto;
import com.example.newsfeed.member.entity.Member;
import com.example.newsfeed.member.repository.MemberRepository;
import com.example.newsfeed.util.AuthenticatedMemberUtil;
import org.springframework.http.ResponseEntity;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/blocks")
public class BlockController {

    private final BlockRepository blockRepository;
    private final MemberRepository memberRepository;

    public BlockController(BlockRepository blockRepository, MemberRepository memberRepository) {
        this.blockRepository = blockRepository;
        this.memberRepository = memberRepository;
    }

    // 토글 (차단 <-> 해제)
    @PostMapping("/{memberId}")
    @Transactional
    public ResponseEntity<CommonResponse<Map<String, Object>>> toggle(@PathVariable Long memberId) {
        Long me = AuthenticatedMemberUtil.getAuthenticatedMemberId();
        boolean blocked;
        if (blockRepository.existsByBlocker_IdAndBlocked_Id(me, memberId)) {
            blockRepository.deleteByBlockerAndBlocked(me, memberId);
            blocked = false;
        } else {
            blockRepository.save(Block.builder()
                    .blocker(memberRepository.getReferenceById(me))
                    .blocked(memberRepository.getReferenceById(memberId))
                    .build());
            blocked = true;
        }
        return ResponseEntity.ok(new CommonResponse<>("차단 상태 변경", Map.of("memberId", memberId, "blocked", blocked)));
    }

    // 차단 여부
    @GetMapping("/{memberId}")
    public ResponseEntity<CommonResponse<Map<String, Object>>> isBlocked(@PathVariable Long memberId) {
        Long me = AuthenticatedMemberUtil.getAuthenticatedMemberIdOrNull();
        boolean blocked = me != null && blockRepository.existsByBlocker_IdAndBlocked_Id(me, memberId);
        return ResponseEntity.ok(new CommonResponse<>("차단 여부", Map.of("memberId", memberId, "blocked", blocked)));
    }

    // 내가 차단한 목록
    @GetMapping
    public ResponseEntity<CommonResponse<List<FriendMemberDto>>> myBlocks() {
        Long me = AuthenticatedMemberUtil.getAuthenticatedMemberId();
        List<Long> ids = blockRepository.blockedIdsOrdered(me);
        Map<Long, Member> byId = memberRepository.findAllById(ids).stream()
                .collect(Collectors.toMap(Member::getId, m -> m));
        List<FriendMemberDto> out = ids.stream().map(byId::get).filter(Objects::nonNull)
                .map(FriendMemberDto::new).toList();
        return ResponseEntity.ok(new CommonResponse<>("차단 목록", out));
    }
}
