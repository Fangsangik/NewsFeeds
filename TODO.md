# TODO — 다음 할 일

> 2026-07-29 세션 종료 시점. 우선순위 순. 상세 설계는 `docs/superpowers/specs|plans/2026-07-29-*` 참고.

## 🔴 포폴 마무리 (바로 다음)

- [ ] **데모 GIF/영상** — 로그인 → 피드(무한스크롤) → 디테일(좋아요/댓글) → DM 플로우 화면 녹화.
      README 상단에 임베드. (Phase 3 Task 3.2, 유일하게 안 끝난 항목)

## 🟡 성능 케이스 스터디 심화 (측정 정밀화 후 R5/R6)

- [ ] **multi-쌍 k6 하네스** — 현재 단일 토큰·단일 쌍이라 대화가 비선택적 → 읽기 인덱스/캐시 효과 측정 불가.
      `loadtest/jmx/users.csv`처럼 여러 sender/receiver를 돌려 현실적 분포로 재구성. (R4·R6 측정의 전제)
- [ ] **라운드 간 교란 제거** — 라운드마다 `TRUNCATE message`로 DB 크기 고정 + 각 셀 반복 측정 후 중앙값.
- [ ] **R5 `@Async` 푸시 분리** — `MessageService.sendMessage`의 `convertAndSendToUser`를 응답 경로에서 제외
      (`@EnableAsync` + `ThreadPoolTaskExecutor`). 현 push는 in-memory·fire-and-forget이라 효과 작을 수 있음 →
      외부 STOMP relay 전제에서 측정하면 의미 있음.
- [ ] **R6 Redis hot-read 캐시** — `LikeServiceImpl.getLikeCount`에 `@Cacheable`/`@CacheEvict`.
      like 중심 부하 시나리오 별도 작성 필요(DM 시나리오로는 안 잡힘).
- [ ] **R2 트레이드오프 보완** — claims 인증은 로그아웃 즉시 무효화 불가. Redis 토큰 블랙리스트 병행 검토.

## 🟢 제품 확장 (HANDOFF.md의 MEDIUM/LOW, 데모 풍부하게)

- [x] 프로필 화면 `#/profile/:memberId` (게시물 그리드) — 2026-08-25
- [x] 친구 화면 `#/friends` (받은/보낸/목록/친구찾기 4탭 + 수락) — 2026-08-25
- [x] DM 화면 `#/dm` (STOMP, 친구 기반 대화) — 2026-08-25
- [x] 카카오 로그인 버튼 + authorize/callback 코드 플로우 — 2026-08-25 (동작엔 카카오 콘솔에 `http://localhost:8080/callback` redirect URI 등록 필요)
- [ ] 아바타 이미지 렌더(`Member.image`), favicon 추가
- [ ] 이미지 업로드 리사이즈(1080px 이내)

## ⚪ 품질/CI (여유 될 때)

- [ ] JUnit 통합테스트 + Testcontainers(MySQL/Redis)로 e2e.sh 대체 → CI 연동
- [ ] `Friend*`/`Kakao*` 컨트롤러 응답을 `CommonResponse`로 통일 (프런트 언래핑 정합)
- [ ] `@ExceptionHandler(Exception.class)` catch-all 세분화 (5xx를 400으로 가리는 문제)
- [ ] (선택) 클라우드 배포로 라이브 URL — 현재는 스코프 밖(docker compose 재현성으로 대체)

## ✅ 2026-08-25 완료

- 2계정(Alice/Bob) 브라우저 E2E 검증 → 버그 10건(B1~B10) 수정·재검증 (TROUBLESHOOTING.md 참고)
- 게시 위치 선택화, 프로필 피드 복구, 친구/DM 상대 정보 정정(DM 실제 전달), 댓글 실명+대댓글 UI
- UI: 상세 6:4 비율, DM 헤더 겹침·시간대(9h) 수정
- README 기술 스택/기능 현행화 (실시간 DM=인메모리 SimpleBroker, Redis 실제 범위 명시)

## ✅ 2026-07-29 완료

- Phase 0: 6주치 미커밋 → 논리 10커밋 정리 + gitignore 정비
- Phase 1: LikeService NPE fix, Feed 응답에 author/feedId 포함
- Phase 2: k6 하네스 재구축, baseline·R2·R3·R4 측정+문서화, 케이스 스터디 종합
- Phase 3: README에 Docker 실행법 + 성능 하이라이트
