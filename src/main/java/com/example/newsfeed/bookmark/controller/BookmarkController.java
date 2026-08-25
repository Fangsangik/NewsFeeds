package com.example.newsfeed.bookmark.controller;

import com.example.newsfeed.bookmark.service.BookmarkService;
import com.example.newsfeed.constants.response.CommonResponse;
import com.example.newsfeed.feed.dto.FeedWithLikeCountDto;
import com.example.newsfeed.util.AuthenticatedMemberUtil;
import org.springframework.data.domain.Page;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/bookmarks")
public class BookmarkController {

    private final BookmarkService bookmarkService;

    public BookmarkController(BookmarkService bookmarkService) {
        this.bookmarkService = bookmarkService;
    }

    // 토글 (저장 <-> 해제)
    @PostMapping("/{feedId}")
    public ResponseEntity<CommonResponse<Map<String, Object>>> toggle(@PathVariable Long feedId) {
        Long me = AuthenticatedMemberUtil.getAuthenticatedMemberId();
        boolean bookmarked = bookmarkService.toggle(feedId, me);
        return ResponseEntity.ok(new CommonResponse<>("저장 상태 변경", Map.of("feedId", feedId, "bookmarked", bookmarked)));
    }

    // 내가 저장했는지 여부
    @GetMapping("/{feedId}")
    public ResponseEntity<CommonResponse<Map<String, Object>>> isBookmarked(@PathVariable Long feedId) {
        Long me = AuthenticatedMemberUtil.getAuthenticatedMemberIdOrNull();
        boolean bookmarked = bookmarkService.isBookmarked(feedId, me);
        return ResponseEntity.ok(new CommonResponse<>("저장 여부", Map.of("feedId", feedId, "bookmarked", bookmarked)));
    }

    // 내가 저장한 게시물 목록
    @GetMapping
    public ResponseEntity<CommonResponse<Page<FeedWithLikeCountDto>>> myBookmarks(@RequestParam(defaultValue = "0") int page,
                                                                                  @RequestParam(defaultValue = "12") int size) {
        Long me = AuthenticatedMemberUtil.getAuthenticatedMemberId();
        return ResponseEntity.ok(new CommonResponse<>("저장한 게시물", bookmarkService.myBookmarks(me, page, size)));
    }
}
