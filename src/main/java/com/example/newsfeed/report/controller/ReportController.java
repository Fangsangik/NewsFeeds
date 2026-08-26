package com.example.newsfeed.report.controller;

import com.example.newsfeed.constants.response.CommonResponse;
import com.example.newsfeed.util.AuthenticatedMemberUtil;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

/**
 * 신고 접수. 별도 심사 파이프라인은 스코프 밖이라, 접수 로그만 남기고 확인 응답을 준다.
 * (운영 단계에서 Report 엔티티/모더레이션 큐로 확장 가능한 최소 구현.)
 */
@Slf4j
@RestController
@RequestMapping("/reports")
public class ReportController {

    public record ReportBody(String targetType, Long targetId, String reason) {}

    @PostMapping
    public ResponseEntity<CommonResponse<Void>> report(@RequestBody ReportBody body) {
        Long me = AuthenticatedMemberUtil.getAuthenticatedMemberId();
        log.info("신고 접수: reporter={}, targetType={}, targetId={}, reason={}",
                me, body.targetType(), body.targetId(), body.reason());
        return ResponseEntity.ok(new CommonResponse<>("신고가 접수되었습니다.", null));
    }
}
