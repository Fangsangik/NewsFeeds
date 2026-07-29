# Newsfeed — 성능 최적화 케이스 스터디 + 풀스택 완성 (포트폴리오)

작성일: 2026-07-29
브랜치: `mvp2/feed`

## 1. 목표 / 포폴 서사

> 인스타그램식 뉴스피드 서비스(SPA · Spring Boot · Docker · 실시간 DM)를 구축하고,
> DM hot path를 **부하테스트 기반으로 5단계 최적화**해 처리량을 **정량적으로 개선**했다.

두 서사를 하나로 엮는다:
- **제품 완성도** — 동작하는 풀스택 제품 (foundation)
- **성능 최적화 케이스 스터디** — 측정 → 가설 → 적용 → 재측정 (differentiator, 핵심)

성공 기준:
- `docker compose up -d` 한 번으로 전체 스택이 뜨고 브라우저에서 전체 플로우가 동작 (재현성)
- `PERFORMANCE.md`에 Baseline → R2~R6 각 라운드의 before/after 표 + Grafana 근거가 누적
- 각 최적화의 효과가 **신뢰할 수 있는 숫자**로 분리 측정됨 (k6 native, ARM 에뮬레이션 결함 제거)
- README + 데모 영상으로 3분 안에 프로젝트를 이해시킬 수 있음

비목표 (YAGNI):
- 클라우드 배포 / 라이브 URL — 하지 않음. `docker compose up` 재현성 + 데모 영상으로 대체
- HANDOFF의 MEDIUM/LOW 화면 확장(프로필/친구/DM 화면, 카카오 버튼 등)은 이번 스코프 밖.
  데모에 필수인 것만 Phase 1에서 처리.

## 2. 현재 상태 (출발점)

- **미커밋**: 6주치 작업(프런트 SPA, Docker화, 파일업로드, WebSocket, 부하테스트 인프라)이 전부 uncommitted.
- **성능 작업 진행도** (`loadtest/PERFORMANCE.md`):
  - ✅ Baseline 측정 완료 — 백엔드 한계 ≈ TPS 290 @ 동시 200명, 그 위로 p95 폭증(643ms→5.2s)
  - ✅ Round 1 (인프라 튜닝: HikariCP 10→50, Tomcat 200→400, JVM 2g) — 고부하 응답시간 ⅓ 단축
  - ⬜ Round 2~6 미완료
- **측정 도구 결함**: `justb4/jmeter:5.5`가 linux/amd64 → ARM 에뮬레이션. 동시 500명 이상에서
  JMeter 컨테이너가 먼저 포화/OOM되어 백엔드 CPU 0~22%로 놀고 있음 → T≥500 측정값 무의미.

## 3. 실행 계획 (4 Phase, 순서 고정)

### Phase 0 — 안전망: 커밋 정리 🔴 (가장 시급, 최우선)

6주치 작업을 논리 단위로 커밋. HANDOFF의 8단계 전략을 따른다:
1. 기존 코드 컴파일 수정 (import 경로, 누락 메서드)
2. JPA 매핑/스키마 fix (`Member.messages` mappedBy, `JwtToken` 컬럼 길이)
3. `FeedServiceImpl` 부정 조건 버그 fix
4. SecurityFilterChain + Multipart + 파일 업로드
5. 프런트엔드 SPA
6. Docker 환경 (Dockerfile, compose, dockerignore)
7. 비번 룰 강화 + GET path 가드
8. e2e 스크립트 + 부하테스트 인프라 + 문서

주의:
- `.env`(카카오/JWT 시크릿 평문)는 커밋하지 않는다. `.gitignore` 확인 후 `.env.example`만.
- `loadtest/results/report/**` (JMeter HTML 리포트, bower_components 수백 파일)와 `.omc/`는
  `.gitignore`에 추가해 커밋에서 제외. `result.jtl`/`*.log` 같은 산출물도 제외, 요약 표는 `PERFORMANCE.md`에 텍스트로.

### Phase 1 — 데모 신뢰성: 화면 깨뜨리는 버그만 fix 🟡

데모/영상에서 티나는 것만 최소 범위로.
1. **LikeService NPE** — 좋아요 0개 피드에 `GET /likes/{feedId}` 시 500.
   조치: JPQL `COALESCE(SUM(l.likeCount), 0L)` 또는 서비스에서 null guard.
2. **Feed 응답에 작성자 정보 포함** — `FeedWithLikeCountDto`/`FeedResponseDto`에
   `feedId`, `author{id, name, image}` 추가. 프런트의 어색한 `/members/{feedId}/member` 호출 제거.
3. **단일 세션 정책** — `AuthService#login`의 `existsByMemberEmailNot(email)` 분기가
   "다른 누가 로그인 중이면 새 로그인 전면 거부" → 멀티유저 데모에서 서로 막힘.
   **결정: 옵션 B** — `existsByMemberEmail(email)`로 바꿔 **본인의 기존 토큰만** 정리하고 재발급.
   누구나 동시 로그인 가능, 본인은 단일 세션 유지. (e2e의 `db_truncate_tokens` 우회 제거 가능)

### Phase 2 — 성능 케이스 스터디: k6 하네스 + R2~R6 ⭐ (차별화 핵심)

측정 프로토콜 (모든 라운드 공통):
- **도구**: k6 native (`brew install k6`). 기존 `dm.jmx` 시나리오(POST 70% / GET 30%, 단일 토큰)를
  `loadtest/k6/dm.js`로 포팅.
- **출력**: k6 → InfluxDB(db=k6) → 기존 Grafana. (JMeter Backend Listener 대시보드는 참고용 보존)
- **시나리오**: step-down 또는 ramping-vus. 각 스테이지 최소 duration 120s + ramp 30s
  (Round 1에서 관측된 단발 스냅샷 노이즈 완화).
- **스냅샷**: 각 스테이지 mid-run에 `docker stats --no-stream nf-app nf-mysql nf-redis`.
- **검증**: 변경이 실제 적용됐는지 메트릭으로 확인 (예: `hikaricp_connections_max`, 쿼리 수).

라운드 (한 번에 하나씩 적용 → 재측정 → baseline과 나란히 비교):
- **R2** JwtFilter DB hit 제거 — JWT claims만으로 Principal 생성. 매 요청 member SELECT 1 제거.
  위치: `JwtFilter`/`JwtProvider`, `UserDetailsServiceImpl`. 비교 지표: MySQL CPU, `hikaricp_connections_active`.
- **R3** MessageService SELECT 2 제거 — `getReferenceById(id)`로 FK만 set (sender/receiver 조회 생략).
  위치: `MessageService`. 비교: POST /messages TPS, MySQL CPU.
- **R4** Message 인덱스 — `(sender_id, receiver_id, created_at)` 복합 + `(receiver_id, read_status)`.
  비교: GET /messages/with/* p95.
- **R5** `@Async` 푸시 — `convertAndSendToUser`를 응답 경로에서 분리 (`@EnableAsync`).
  비교: POST /messages p95.
- **R6** Redis 캐시 — hot read(예: `getLikeCount`) DB hit 제거.
  비교: DB CPU 추가 감소.

각 라운드 종료 시 `PERFORMANCE.md`의 "라운드 결과 기록 양식"에 표 + Grafana 스크린샷 + 결론을 채운다.
Phase 2 종료 시 전체를 **문제 → 가설 → 실험 → 결과 → 결론** 케이스 스터디 문서로 정리.

### Phase 3 — 제품 폴리시: README + 데모 (배포 없음)

- **README.md** — 프로젝트 소개, 아키텍처 다이어그램, 기술 스택, `docker compose up -d` 실행법,
  성능 결과 하이라이트 표(Baseline vs 최종), 성능 케이스 스터디 문서 링크.
- **데모 영상/GIF** — 로그인 → 피드 → 디테일(좋아요/댓글) → (가능하면) DM 전체 플로우.
- 재현성 체크: 클린 체크아웃에서 `docker compose up -d` → e2e 47/47 → 브라우저 수동 클릭.

## 4. 리스크 / 알려진 제약

- **측정 환경 변동성**: 로컬 머신 부하(다른 프로세스)에 측정이 흔들림. 각 라운드는 같은 조건에서
  연속 측정하고, 애매하면 재측정. 절대값보다 **같은 하네스에서의 상대 변화**를 신뢰한다.
- **R2 보안 주의**: JWT claims만으로 인증 시, 토큰 무효화(로그아웃/강제만료)가 DB 확인 없이는
  즉시 반영 안 됨. access token 만료를 짧게 유지하거나 블랙리스트(Redis) 병행 여부를 R2에서 판단.
- **DDL-auto**: 컬럼 길이 축소/인덱스는 `ddl-auto=update`가 자동 반영 안 할 수 있음 → R4는 명시적
  `ALTER TABLE ... ADD INDEX` 또는 마이그레이션으로.

## 5. 산출물

- 논리 단위로 커밋된 git 히스토리
- 데모 가능한 `docker compose up` 스택 + e2e 47/47
- `loadtest/k6/dm.js` (신뢰성 있는 측정 하네스)
- `loadtest/PERFORMANCE.md` — Baseline + R1~R6 before/after 누적 + 케이스 스터디 결론
- `README.md` — 아키텍처 + 실행법 + 성능 하이라이트
- 데모 영상/GIF
