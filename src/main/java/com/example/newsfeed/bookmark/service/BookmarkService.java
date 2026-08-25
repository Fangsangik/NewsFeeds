package com.example.newsfeed.bookmark.service;

import com.example.newsfeed.bookmark.entity.Bookmark;
import com.example.newsfeed.bookmark.repository.BookmarkRepository;
import com.example.newsfeed.exception.ErrorCode;
import com.example.newsfeed.exception.NotFoundException;
import com.example.newsfeed.feed.dto.FeedWithLikeCountDto;
import com.example.newsfeed.feed.repository.FeedRepository;
import com.example.newsfeed.member.repository.MemberRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class BookmarkService {

    private final BookmarkRepository bookmarkRepository;
    private final FeedRepository feedRepository;
    private final MemberRepository memberRepository;

    public BookmarkService(BookmarkRepository bookmarkRepository, FeedRepository feedRepository, MemberRepository memberRepository) {
        this.bookmarkRepository = bookmarkRepository;
        this.feedRepository = feedRepository;
        this.memberRepository = memberRepository;
    }

    /** 토글: 없으면 저장, 있으면 해제. 최종 상태(bookmarked)를 반환. */
    @Transactional
    public boolean toggle(Long feedId, Long memberId) {
        if (bookmarkRepository.existsByMember_IdAndFeed_Id(memberId, feedId)) {
            bookmarkRepository.deleteByMemberAndFeed(memberId, feedId);
            return false;
        }
        var feed = feedRepository.findById(feedId)
                .orElseThrow(() -> new NotFoundException(ErrorCode.NOT_FOUND_NEWSFEED));
        bookmarkRepository.save(Bookmark.builder()
                .feed(feed)
                .member(memberRepository.getReferenceById(memberId))
                .build());
        return true;
    }

    @Transactional(readOnly = true)
    public boolean isBookmarked(Long feedId, Long memberId) {
        return memberId != null && bookmarkRepository.existsByMember_IdAndFeed_Id(memberId, feedId);
    }

    @Transactional(readOnly = true)
    public Page<FeedWithLikeCountDto> myBookmarks(Long memberId, int page, int size) {
        return bookmarkRepository.findMyBookmarks(memberId, PageRequest.of(page, size));
    }
}
