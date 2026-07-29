# Newsfeed 성능 최적화 + 풀스택 완성 Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** 미커밋 6주치 작업을 안전하게 커밋하고, 데모 신뢰성을 확보한 뒤, DM hot path를 k6 기반으로 5단계 최적화해 정량 개선을 문서화한다.

**Architecture:** Spring Boot 3.4 / Java 17 백엔드 + 정적 SPA + Docker Compose 스택(MySQL/Redis/InfluxDB/Grafana). 성능 라운드는 "한 번에 한 변경 → k6 재측정 → baseline과 비교" 실험 루프로 진행한다.

**Tech Stack:** Spring Boot, Spring Security(JWT), JPA/Hibernate, MySQL 8, Redis 7, STOMP/WebSocket, Docker Compose, k6, InfluxDB 1.8, Grafana 11.

## Global Constraints

- 회귀 게이트는 `./scripts/e2e.sh` (현재 47/47 PASS). 모든 코드 변경 후 반드시 재실행해 47/47 유지 확인.
- 백엔드 코드 변경 반영: `docker compose up -d --build app`. 프런트 변경은 브라우저 강제 새로고침.
- 시크릿(`.env`)은 절대 커밋하지 않는다. `.env`는 이미 `.gitignore`에 있음.
- 커밋 메시지는 기존 히스토리 컨벤션(`feat:`/`refactor:`/`docs:`) 유지.
- 성능 측정은 절대값보다 **동일 하네스에서의 상대 변화**를 신뢰한다. 애매하면 재측정.
- 포트: app 8080, MySQL 3307, Redis 6380, InfluxDB 8086, Grafana 3000.

---

## Phase 0 — 안전망: 6주치 작업 커밋 정리 🔴

목표: 현재 uncommitted 상태를 논리 단위 커밋으로 안전하게 만든다. 산출물/시크릿은 제외.

### Task 0.1: .gitignore 보강 (산출물·리포트·OMC 제외)

**Files:**
- Modify: `.gitignore`

- [ ] **Step 1: `.gitignore`에 아래 항목 추가** (이미 있는 `.env`, `logs/`, `loadtest/results/`는 유지)

```
# OMC orchestration state
.omc/

# JMeter generated HTML report (bower_components 수백 파일) — 요약은 PERFORMANCE.md에 텍스트로 보존
loadtest/results/report/
```

- [ ] **Step 2: 추적 대상 확인**

Run: `git status --short && git ls-files loadtest/results | head`
Expected: `loadtest/results/**`, `.omc/**`가 `??`(untracked)이며 커밋 후보에서 빠짐. `.env` 미표시.

- [ ] **Step 3: 커밋**

```bash
git add .gitignore
git commit -m "chore: 산출물/리포트/OMC 상태 gitignore 추가"
```

### Task 0.2: 논리 단위로 나머지 작업 커밋 (8분할)

**Files:** 전체 워킹트리 (아래 그룹별로 `git add`)

각 그룹을 개별 커밋한다. 한 그룹 add 후 `git status`로 의도한 파일만 스테이징됐는지 확인하고 커밋.

- [ ] **Step 1: 컴파일 수정 그룹** — import 경로/누락 메서드/신규 유틸

```bash
git add src/main/java/com/example/newsfeed/util/AuthenticatedMemberUtil.java \
        src/main/java/com/example/newsfeed/auth/jwt/service/JwtProvider.java \
        src/main/java/com/example/newsfeed/auth/jwt/service/UserDetailsImpl.java \
        src/main/java/com/example/newsfeed/auth/jwt/repository/TokenRepository.java \
        src/main/java/com/example/newsfeed/auth/jwt/dto/JwtMemberDto.java
git commit -m "fix: 컴파일 오류 수정 (import 경로, 누락 메서드 정의)"
```

- [ ] **Step 2: JPA 매핑/스키마 fix**

```bash
git add src/main/java/com/example/newsfeed/member/entity/Member.java \
        src/main/java/com/example/newsfeed/auth/jwt/entity/JwtToken.java \
        src/main/java/com/example/newsfeed/message/entity/Message.java
git commit -m "fix: JPA 매핑 정합 (Member.messages mappedBy, JwtToken 컬럼 길이)"
```

- [ ] **Step 3: FeedServiceImpl 부정 조건 버그 fix**

```bash
git add src/main/java/com/example/newsfeed/feed/service/FeedServiceImpl.java \
        src/main/java/com/example/newsfeed/feed/repository/FeedRepository.java \
        src/main/java/com/example/newsfeed/feed/dto/FeedWithLikeCountDto.java
git commit -m "fix: FeedServiceImpl update/delete 존재검사 조건 반전 버그 수정"
```

- [ ] **Step 4: 보안/멀티파트/파일 업로드 + WebSocket**

```bash
git add src/main/java/com/example/newsfeed/auth/config/SecurityConfig.java \
        src/main/java/com/example/newsfeed/constants/config/ \
        src/main/java/com/example/newsfeed/file/ \
        src/main/java/com/example/newsfeed/auth/jwt/filter/JwtHandshakeHandler.java \
        src/main/java/com/example/newsfeed/auth/jwt/filter/JwtHandshakeInterceptor.java \
        src/main/java/com/example/newsfeed/message/ \
        src/main/resources/application.yml
git commit -m "feat: 파일 업로드 + STOMP WebSocket 인증 핸드셰이크 + 멀티파트 설정"
```

- [ ] **Step 5: 프런트엔드 SPA**

```bash
git add src/main/resources/static/
git commit -m "feat: 뉴스피드 SPA (로그인/회원가입/피드/디테일)"
```

- [ ] **Step 6: Docker 환경**

```bash
git add Dockerfile docker-compose.yml .dockerignore build.gradle
git commit -m "feat: Docker Compose 스택 (app/mysql/redis/influxdb/grafana)"
```

- [ ] **Step 7: 회원/인증 보강 (비번 룰, path 가드, 검색 DTO)**

```bash
git add src/main/java/com/example/newsfeed/member/ \
        src/main/java/com/example/newsfeed/like/controller/LikeController.java \
        src/main/java/com/example/newsfeed/exception/GlobalExceptionController.java
git commit -m "feat: 비밀번호 규칙 강화 + GET path 정규식 가드 + 예외 로깅"
```

- [ ] **Step 8: 부하테스트 하네스 + 모니터링 + 문서**

```bash
git add scripts/ loadtest/PERFORMANCE.md loadtest/jmx/ monitoring/ \
        TROUBLESHOOTING.md HANDOFF.md
git commit -m "test: JMeter 부하테스트 하네스 + Grafana 모니터링 + 성능 문서"
```

- [ ] **Step 9: 남은 파일 확인 및 정리**

Run: `git status --short`
Expected: 빈 출력(또는 의도적으로 무시된 `.env`, `logs/`, `loadtest/results/`, `.omc/`만). 남은 소스가 있으면 적절한 그룹으로 커밋.

---

## Phase 1 — 데모 신뢰성: 화면 깨뜨리는 버그 fix 🟡

주의: `AuthService.login`의 단일세션 정책은 **이미 옵션 B로 구현됨**(`deleteByMember`) — 별도 작업 불필요.

### Task 1.1: LikeService NPE fix (좋아요 0개 피드 조회 500 방지)

**Files:**
- Modify: `src/main/java/com/example/newsfeed/like/repository/LikeRepository.java:14-15`
- Test: `scripts/e2e.sh` (GET like count 케이스)

**Interfaces:**
- Produces: `LikeRepository.countByFeedId(Long)` — 좋아요 0개일 때 `null`이 아니라 `0L` 반환.

- [ ] **Step 1: 현재 NPE 재현** (스택 실행 중 가정)

Run: `curl -s -o /dev/null -w "%{http_code}\n" -H "Authorization: Bearer $TOKEN" http://localhost:8080/likes/<좋아요0개인_feedId>`
Expected: 현재 500 (또는 로그에 `Cannot invoke "java.lang.Long.intValue()" because "count" is null`)

- [ ] **Step 2: JPQL에 COALESCE 적용**

`LikeRepository.java` 14행 쿼리를 아래로 교체:

```java
    @Query("select coalesce(sum(l.likeCount), 0L) from Like l where l.feed.id = :feedId")
    Long countByFeedId(@Param("feedId") Long feedId);
```

- [ ] **Step 3: 재빌드 및 재현 확인**

Run: `docker compose up -d --build app` 후 Step 1 curl 재실행
Expected: 200, 바디의 likeCount = 0

- [ ] **Step 4: e2e 회귀**

Run: `./scripts/e2e.sh`
Expected: 47/47 PASS

- [ ] **Step 5: 커밋**

```bash
git add src/main/java/com/example/newsfeed/like/repository/LikeRepository.java
git commit -m "fix: 좋아요 0개 피드 조회 시 NPE 방지 (COALESCE SUM)"
```

### Task 1.2: Feed 응답에 작성자 정보 + feedId 포함

**Files:**
- Modify: `src/main/java/com/example/newsfeed/feed/dto/FeedWithLikeCountDto.java`
- Modify: `src/main/java/com/example/newsfeed/feed/service/FeedServiceImpl.java` (매핑 지점)
- Modify: `src/main/resources/static/js/detail.js` (별도 작성자 호출 제거)

**Interfaces:**
- Produces: Feed 조회 응답 JSON에 `feedId`(Long), `author{id, name, image}` 포함.

- [ ] **Step 1: 현재 응답 확인**

Run: `curl -s -H "Authorization: Bearer $TOKEN" http://localhost:8080/feeds/likecount | head -c 400`
Expected: 현재 author 정보/feedId 없음 확인.

- [ ] **Step 2: DTO에 필드 추가**

`FeedWithLikeCountDto`에 `feedId`, 중첩 `AuthorDto{Long id, String name, String image}` 추가하고 매핑 시 Feed의 작성자(Member)에서 채운다. (기존 필드/빌더 패턴 유지)

- [ ] **Step 3: 서비스 매핑 반영**

`FeedServiceImpl`의 해당 DTO 생성부에서 author와 feedId를 채운다. 작성자 로딩이 N+1을 유발하면 fetch join 또는 이미 로딩된 연관 사용.

- [ ] **Step 4: 프런트 정리**

`detail.js`에서 `/members/{feedId}/member` 별도 호출 제거하고 피드 응답의 `author` 사용.

- [ ] **Step 5: 재빌드 + 브라우저 확인 + e2e**

Run: `docker compose up -d --build app && ./scripts/e2e.sh`
Expected: 47/47. 브라우저 디테일 화면에 작성자 정상 표시, 네트워크 탭에 `/member` 별도 호출 없음.

- [ ] **Step 6: 커밋**

```bash
git add src/main/java/com/example/newsfeed/feed/ src/main/resources/static/js/detail.js
git commit -m "feat: 피드 응답에 author/feedId 포함, 프런트 중복 호출 제거"
```

---

## Phase 2 — 성능 케이스 스터디: k6 하네스 + R2~R6 ⭐

### Task 2.1: k6 설치 + dm 시나리오 포팅 + InfluxDB 연동

**Files:**
- Create: `loadtest/k6/dm.js`
- Create: `scripts/k6-load.sh`
- Modify: `docker-compose.yml` (InfluxDB에 `k6` db 보장 — 기존 influxdb 컨테이너 재사용)
- Modify: `monitoring/grafana/` (k6 데이터소스/대시보드는 기존 influxdb 재사용, 필요시 대시보드 추가)

**Interfaces:**
- Produces: `./scripts/k6-load.sh` — step-down 부하를 k6로 실행, 결과를 InfluxDB(db=k6)로 전송.

- [ ] **Step 1: k6 설치**

Run: `brew install k6 && k6 version`
Expected: 버전 출력.

- [ ] **Step 2: dm 시나리오 k6 스크립트 작성**

`loadtest/k6/dm.js` — 기존 `loadtest/jmx/dm.jmx`와 동일 시나리오: 로그인으로 토큰 1개 획득 → 70% `POST /messages` (body: receiverId, content), 30% `GET /messages/with/{peer}`. ramping-vus stages: 50→100→200→500→1000, 각 stage duration 120s + ramp 30s. thresholds에 `http_req_failed`, `http_req_duration{p(95)}` 기록.

```javascript
import http from 'k6/http';
import { check } from 'k6';
import { Rate } from 'k6/metrics';

const BASE = __ENV.BASE || 'http://localhost:8080';
const TOKEN = __ENV.TOKEN;          // 사전 발급 토큰 주입
const PEER = __ENV.PEER || '2';
const errors = new Rate('errors');

export const options = {
  stages: [
    { duration: '30s', target: 50 },  { duration: '120s', target: 50 },
    { duration: '30s', target: 100 }, { duration: '120s', target: 100 },
    { duration: '30s', target: 200 }, { duration: '120s', target: 200 },
    { duration: '30s', target: 500 }, { duration: '120s', target: 500 },
    { duration: '30s', target: 1000 },{ duration: '120s', target: 1000 },
    { duration: '30s', target: 0 },
  ],
  thresholds: { http_req_failed: ['rate<0.05'] },
};

const headers = { Authorization: `Bearer ${TOKEN}`, 'Content-Type': 'application/json' };

export default function () {
  let res;
  if (Math.random() < 0.7) {
    res = http.post(`${BASE}/messages`,
      JSON.stringify({ receiverId: Number(PEER), content: 'load test' }), { headers });
  } else {
    res = http.get(`${BASE}/messages/with/${PEER}`, { headers });
  }
  errors.add(res.status >= 400);
  check(res, { 'status < 400': (r) => r.status < 400 });
}
```

(POST body 필드명은 실제 `MessageController`/`MessageRequestDto` 시그니처에 맞춰 확정할 것.)

- [ ] **Step 3: 실행 래퍼 작성**

`scripts/k6-load.sh` — 토큰 발급(로그인 curl) → `k6 run --out influxdb=http://localhost:8086/k6 loadtest/k6/dm.js` → stage별 `docker stats --no-stream nf-app nf-mysql nf-redis`를 백그라운드로 주기 캡처하여 `loadtest/results/k6/<round>/`에 저장. 실행권한 `chmod +x`.

- [ ] **Step 4: 스모크 실행**

Run: `TOKEN=$(...) ./scripts/k6-load.sh smoke` (짧은 stage로 1회)
Expected: k6 요약 출력 + InfluxDB db=k6에 데이터 유입 (`curl 'http://localhost:8086/query?db=k6&q=SHOW+MEASUREMENTS'`).

- [ ] **Step 5: 커밋**

```bash
git add loadtest/k6/dm.js scripts/k6-load.sh docker-compose.yml monitoring/
git commit -m "test: k6 native 부하 하네스 (dm 시나리오, InfluxDB 연동)"
```

### Task 2.2: Baseline 재측정 (k6, 신뢰성 있는 고부하 포함)

- [ ] **Step 1: 클린 상태 준비** — `docker compose exec -T mysql mysql -uroot -p1234 newsfeed -e "DELETE FROM jwt_token;"`, 시드 데이터 확인.
- [ ] **Step 2: 전체 라운드 실행** — `./scripts/k6-load.sh baseline-k6`
- [ ] **Step 3: 결과 기록** — `PERFORMANCE.md`에 "Round 0 — k6 Baseline (2026-07-29)" 섹션 추가. T별 TPS/p50/p95/p99/err% 표 + docker stats. **이 값이 이후 모든 라운드의 비교 기준.**
- [ ] **Step 4: 커밋** — `git add loadtest/PERFORMANCE.md && git commit -m "test: k6 baseline 재측정 (신뢰성 있는 고부하 구간 포함)"`

### Task 2.3: R2 — JwtFilter DB hit 제거

**Files:**
- Modify: `src/main/java/com/example/newsfeed/auth/jwt/filter/JwtFilter.java:48-70`

- [ ] **Step 1: 변경** — `authenticate()`에서 `userDetailsService.loadUserByUsername(username)` (member SELECT) 제거. 대신 토큰 claims(`getUsername`, `getMemberId`, role)로 `UserDetailsImpl` 또는 경량 principal을 직접 구성해 SecurityContext에 주입. `AuthenticatedMemberUtil`이 memberId를 principal에서 꺼내는 경로가 유지되는지 확인.
- [ ] **Step 2: 보안 확인** — 로그아웃/토큰 무효화가 DB 확인 없이 즉시 반영 안 되는 트레이드오프를 인지. access token 만료가 짧게 유지되는지(`JwtProvider`) 점검, 필요 시 후속(R6 Redis 블랙리스트) 메모.
- [ ] **Step 3: e2e 회귀** — `docker compose up -d --build app && ./scripts/e2e.sh` → 47/47.
- [ ] **Step 4: 재측정** — `./scripts/k6-load.sh r2-jwt`
- [ ] **Step 5: 기록** — `PERFORMANCE.md` "Round 2" 표(vs baseline Δ) + `hikaricp_connections_active`, MySQL CPU 비교.
- [ ] **Step 6: 커밋** — `git add ... && git commit -m "perf(R2): JwtFilter 매 요청 DB 조회 제거 (JWT claims 인증)"`

### Task 2.4: R3 — MessageService SELECT 2 제거

**Files:**
- Modify: `src/main/java/com/example/newsfeed/message/service/MessageService.java:36-42`

- [ ] **Step 1: 변경** — `sendMessage`에서 `memberRepository.findById(senderId/receiverId)` 2건 제거, `memberRepository.getReferenceById(id)`로 프록시만 얻어 `Message` FK set. Message 생성자가 Member 참조만 필요하므로 실제 로딩 불필요.
- [ ] **Step 2: 유효성 주의** — 존재하지 않는 receiverId일 때 동작(FK 위반 예외 처리) 확인, 필요 시 `existsById` 경량 체크로 대체.
- [ ] **Step 3: e2e 회귀** → 47/47.
- [ ] **Step 4: 재측정** — `./scripts/k6-load.sh r3-getref`
- [ ] **Step 5: 기록** — POST /messages TPS, MySQL CPU 비교.
- [ ] **Step 6: 커밋** — `git commit -m "perf(R3): 메시지 전송 시 sender/receiver SELECT 제거 (getReferenceById)"`

### Task 2.5: R4 — Message 복합 인덱스

**Files:**
- Create: `src/main/resources/db/migration/` 또는 실행 스크립트 (DDL)

- [ ] **Step 1: 인덱스 추가** — `ALTER TABLE message ADD INDEX idx_msg_conv (sender_id, receiver_id, created_at), ADD INDEX idx_msg_unread (receiver_id, read_status);` (`ddl-auto=update`는 인덱스 자동 생성 보장 못 하므로 명시 적용.)
- [ ] **Step 2: 적용 확인** — `SHOW INDEX FROM message;`
- [ ] **Step 3: e2e 회귀** → 47/47.
- [ ] **Step 4: 재측정** — `./scripts/k6-load.sh r4-index` (GET /messages/with/* 비중 있는 시나리오)
- [ ] **Step 5: 기록** — GET p95 비교.
- [ ] **Step 6: 커밋** — DDL 파일 커밋 + `PERFORMANCE.md`.

### Task 2.6: R5 — @Async STOMP 푸시 분리

**Files:**
- Modify: `src/main/java/com/example/newsfeed/message/service/MessageService.java:45-52`
- Modify: 설정 클래스에 `@EnableAsync` + `TaskExecutor` 빈

- [ ] **Step 1: 변경** — `convertAndSendToUser` 호출을 `@Async` 메서드로 분리해 POST 응답 경로에서 제외. 별도 `@Component` 또는 self-injection 주의(프록시). `ThreadPoolTaskExecutor` 빈 정의.
- [ ] **Step 2: e2e 회귀** → 47/47 (푸시는 비동기이므로 응답에 영향 없어야 함).
- [ ] **Step 3: 재측정** — `./scripts/k6-load.sh r5-async`
- [ ] **Step 4: 기록** — POST /messages p95 비교.
- [ ] **Step 5: 커밋** — `git commit -m "perf(R5): STOMP 푸시 @Async 분리로 응답 지연 감소"`

### Task 2.7: R6 — Redis hot read 캐시

**Files:**
- Modify: `src/main/java/com/example/newsfeed/like/service/LikeServiceImpl.java`
- Modify: 캐시 설정 (`@EnableCaching`, RedisCacheManager)

- [ ] **Step 1: 변경** — `getLikeCount`에 `@Cacheable`(Redis) 적용, `like`/`disLike`에서 `@CacheEvict`. Redis는 이미 compose에 있음(6380).
- [ ] **Step 2: e2e 회귀** → 47/47 (좋아요 증감 후 카운트 정합 확인).
- [ ] **Step 3: 재측정** — `./scripts/k6-load.sh r6-redis` (like count read 포함 시나리오면 효과 관측)
- [ ] **Step 4: 기록** — DB CPU 추가 감소 비교.
- [ ] **Step 5: 커밋** — `git commit -m "perf(R6): 좋아요 카운트 Redis 캐시로 hot read DB hit 제거"`

### Task 2.8: 케이스 스터디 종합

- [ ] **Step 1:** `PERFORMANCE.md` 상단에 **문제 → 가설 → 실험 → 결과 → 결론** 요약 + Baseline vs 최종 하이라이트 표 + 라운드별 누적 개선 그래프(Grafana 스크린샷 `docs/` 또는 `loadtest/`에 저장).
- [ ] **Step 2:** 커밋.

---

## Phase 3 — 제품 폴리시: README + 데모 (배포 없음)

### Task 3.1: README 작성

**Files:**
- Modify/Create: `README.md`

- [ ] **Step 1:** 프로젝트 소개, 아키텍처 다이어그램(텍스트/mermaid), 기술 스택, `docker compose up -d` 실행법, 주요 API, **성능 결과 하이라이트 표(Baseline vs 최종)**, 성능 케이스 스터디(`loadtest/PERFORMANCE.md`) 링크.
- [ ] **Step 2:** 클린 체크아웃 재현성 확인 — 새 클론에서 `docker compose up -d` → `curl localhost:8080/` 200 → `./scripts/e2e.sh` 47/47.
- [ ] **Step 3:** 커밋.

### Task 3.2: 데모 영상/GIF

- [ ] **Step 1:** 로그인 → 피드(무한스크롤) → 디테일(좋아요/댓글) → 가능하면 DM 전체 플로우를 화면 녹화. GIF로 변환해 README에 임베드하거나 링크.
- [ ] **Step 2:** 커밋(GIF 용량 크면 링크만).

---

## Self-Review 결과

- **Spec coverage:** Phase 0(커밋)/1(버그)/2(R2~R6 성능)/3(README·데모) — 스펙의 4 Phase 전부 태스크로 매핑됨. 단일세션(옵션 B)은 이미 구현 확인되어 태스크 제외(스펙의 결정과 일치).
- **Placeholder scan:** R2~R6의 정확한 diff는 실제 파일을 읽고 확정(타겟 파일/행 명시). 측정값 표는 실행 후 채우는 게 정상(실험 로그).
- **Type consistency:** `countByFeedId(Long)→Long`, `getReferenceById` 사용, k6 `TOKEN/PEER/BASE` env 계약 일관.
- **알려진 리스크:** 측정 환경 변동성, R2 토큰 무효화 트레이드오프, DDL-auto 인덱스 미반영 — 스펙 §4와 각 태스크에 반영됨.
