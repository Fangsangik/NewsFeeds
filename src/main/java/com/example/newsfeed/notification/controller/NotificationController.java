package com.example.newsfeed.notification.controller;

import com.example.newsfeed.constants.response.CommonResponse;
import com.example.newsfeed.notification.dto.NotificationResponseDto;
import com.example.newsfeed.notification.service.NotificationService;
import com.example.newsfeed.util.AuthenticatedMemberUtil;
import org.springframework.data.domain.Page;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/notifications")
public class NotificationController {

    private final NotificationService notificationService;

    public NotificationController(NotificationService notificationService) {
        this.notificationService = notificationService;
    }

    @GetMapping
    public ResponseEntity<CommonResponse<Page<NotificationResponseDto>>> list(@RequestParam(defaultValue = "0") int page,
                                                                              @RequestParam(defaultValue = "20") int size) {
        Long me = AuthenticatedMemberUtil.getAuthenticatedMemberId();
        return ResponseEntity.ok(new CommonResponse<>("알림 목록", notificationService.list(me, page, size)));
    }

    @GetMapping("/unread-count")
    public ResponseEntity<CommonResponse<Map<String, Object>>> unreadCount() {
        Long me = AuthenticatedMemberUtil.getAuthenticatedMemberId();
        return ResponseEntity.ok(new CommonResponse<>("안읽은 알림 수", Map.of("count", notificationService.unreadCount(me))));
    }

    @PatchMapping("/read-all")
    public ResponseEntity<CommonResponse<Void>> readAll() {
        Long me = AuthenticatedMemberUtil.getAuthenticatedMemberId();
        notificationService.markAllRead(me);
        return ResponseEntity.ok(new CommonResponse<>("모두 읽음 처리", null));
    }
}
