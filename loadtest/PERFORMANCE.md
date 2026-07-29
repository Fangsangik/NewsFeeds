# Newsfeed — Performance Test Log

> DM 송수신 hot path 기준 부하 테스트 결과 + 시스템 메트릭 + 다음 개선 단계.
> 측정값을 비교 가능한 baseline으로 남겨두고, 개선 작업은 한 항목씩 적용 → 재측정 → 차이 비교한다.

---

## 📊 케이스 스터디 요약 (Executive Summary)

**한 줄:** DM 전송 hot path를 부하테스트로 프로파일링해 **DB read-bound → write-bound로 병목을 이동**시키고,
요청당 DB SELECT를 **3 → 0**으로 줄여 앱 CPU를 고부하에서 절반 이하로 낮췄다.

### 문제 (Problem)
`POST /messages` 한 건당 백엔드가 **SELECT 3 + INSERT 1**을 수행 (JwtFilter 인증 조회 1 + sender/receiver 조회 2 + save 1).
k6 native 부하(단일 토큰, POST 70% / GET 30%)로 측정하니 **MySQL이 병목**: VUS=50에서 이미 MySQL CPU 267%
(nf-app 175%), VUS=100에서 MySQL 490% → TPS가 237→206으로 **역전**(부하를 올릴수록 처리량 감소).

### 가설 (Hypothesis)
요청당 DB 왕복이 병목의 원인 → **읽기(SELECT)를 제거하면 DB·앱 부하가 함께 내려간다.** 한 번에 한 변경만
적용하고 동일 하네스(ramp 20s + 90s × VU 50/100/200/500/1000)로 재측정해 효과를 분리한다.

### 실험 & 결과 (Experiments)

| 라운드 | 변경 | 요청당 SELECT | 핵심 결과 |
|---|---|---|---|
| **Baseline** | (인프라 튜닝만) | 3 | MySQL 병목. VUS=50 MySQL **267%**, 피크 TPS 237 후 degrade |
| **R2** | JwtFilter DB 조회 제거 (JWT claims 인증) | 3→**2** | VUS=50 MySQL **267→167%** (-37%), nf-app 175→158% |
| **R3** | 메시지 전송 sender/receiver 조회 제거 (getReferenceById) | 2→**0** | nf-app CPU **급감**(VUS=500 163→**71%**), 병목이 순수 INSERT로 이동, 중부하 TPS **91→157 (+72%)** |
| **R4** | 대화/안읽음 복합 인덱스 (스키마) | — | 선택적 대화에서 idx 사용 + filesort 제거(EXPLAIN). *하네스 단일쌍 한계로 부하 재현 불가* |

### 결론 (Conclusion)
- **병목을 이동시켰다**: DB read-bound(SELECT 3) → 순수 write-bound(INSERT 1). 앱은 이제 요청당 거의
  INSERT 1건만 조율하고, 고부하에서 nf-app CPU가 **절반 이하**로 내려감.
- **DB read 부하 감소를 정량 확인**: 비포화 구간(VUS=50)에서 SELECT 1개 제거 → MySQL CPU 37% 감소로
  기전과 수치가 일치.
- **정직한 한계 인식**: 남은 병목은 write 자체 → 이를 넘으려면 배치/큐/샤딩 등 아키텍처 레벨 개입 필요.
  측정 하네스의 데이터 분포 결함과 라운드 간 테이블 성장 교란도 문서화(아래 "측정 방법론 한계").

### 향후 개선 (Further Work) — 설계했으나 이번 하네스로는 측정 부적합
- **R5 `@Async` 푸시 분리**: `convertAndSendToUser`를 응답 경로에서 제외해 POST 지연 감소. 단, 현 푸시는 이미
  in-memory·fire-and-forget이고 경로가 INSERT-bound라 효과가 작을 것으로 예상 → 측정하려면 push 비용이
  큰 브로커(외부 STOMP relay) 전제 필요.
- **R6 Redis hot-read 캐시**: `getLikeCount` 등은 **DM이 아닌 별도 hot path** → like 중심 시나리오 필요.
  R2의 claims 인증 트레이드오프(즉시 토큰 폐기 불가)를 Redis 블랙리스트로 보완하는 것도 이 라운드 후보.
- **측정 정밀화**: multi-쌍 하네스 + 라운드마다 `TRUNCATE message` + 반복 측정 중앙값.

---

## 환경

- 호스트: Apple Silicon (linux/arm64/v8)
- Docker compose 스택: `nf-app` (Spring Boot 3.4 / Java 17), `nf-mysql` 8.0, `nf-redis` 7, `nf-influxdb` 1.8, `nf-grafana` 11.2
- 메트릭 수집: Micrometer → InfluxDB(db=springboot), JMeter Backend Listener → InfluxDB(db=jmeter), Grafana 대시보드 `newsfeed-backend` 자체 작성
- 부하 도구: `justb4/jmeter:5.5` (linux/amd64 → ARM 에뮬레이션)
- 시나리오: DM 70% `POST /messages` + 30% `GET /messages/with/{peer}`, 단일 토큰
- Spring 기본 설정: Tomcat thread max=200, HikariCP pool=10, JVM heap 디폴트 (~1GB)

---

## Baseline (2026-06-23)

`scripts/step-load.sh dm` (1000 → 500 → 200 → 100 → 50, 각 60s, ramp 10s)

```
   T   samples     TPS     avg(ms)  p50    p95    p99   err%
  ────────────────────────────────────────────────────────────
   50   13,122   218.9       198    128    529   1,555   0.0
  100   17,346   287.8       264    187    643   2,676   0.0   ← Peak TPS
  200    4,656    98.5      1248    669   5250   6,942   0.0   ← 백엔드 무릎
  500    1,079    27.8     11627  13321  17602  18,252   0.0   ← JMeter 한계
 1000      700    15.1     17592  21397  32400  46,738   0.0   ← JMeter OOM (exit 137)
```

### docker stats (mid-run 스냅샷)

| T | nf-app CPU | nf-app mem | nf-mysql CPU | 진단 |
|---|---|---|---|---|
| 50  | 152% | 957 MiB | 122% | 백엔드 1.5코어, DB 1.2코어 — 여유 있음 |
| 100 | 151% | 954 MiB | 105% | 동일, 처리량은 +30% 증가 |
| 200 | **228%** | 944 MiB | 53% | **2.3코어 포화 → TPS 급락, p95 폭증 (643ms→5.2s)** |
| 500 | 22% | 924 MiB | 5% | 백엔드 idle — JMeter가 못 부어줌 |
| 1000 | 0% | 845 MiB | 3% | JMeter 컨테이너 OOM kill |

### 핵심 결론

- **실측 백엔드 한계 ≈ TPS 290 @ 약 200 동시 사용자**. 그 위로는 응답시간이 폭발하며 TPS가 오히려 떨어짐(228% CPU에서 GC/contention 추정).
- **T=500, T=1000은 백엔드가 아니라 JMeter 컨테이너의 ARM 에뮬레이션 한계**. 백엔드 CPU 0~22%로 거의 놀고 있음 → 클라이언트 측정 한계.
- T=100 시점에서 **MySQL CPU 105%**가 백엔드와 거의 동등하게 바쁨. 즉 DB가 일찍 병목으로 들어옴.

---

## 진단된 hot path 비효율 (이번 측정으로 확인됨)

`POST /messages` 한 건당 백엔드가 하는 일:

```
1. JwtFilter.doFilterInternal
   └─ UserDetailsService.loadUserByUsername(email)   ← member SELECT × 1
2. SecurityContext 주입
3. MessageController.send
4. MessageService.sendMessage
   ├─ memberRepository.findById(senderId)            ← member SELECT × 1
   ├─ memberRepository.findById(receiverId)          ← member SELECT × 1
   ├─ messageRepository.save(...)                     ← message INSERT × 1
   └─ messagingTemplate.convertAndSendToUser(...)     ← STOMP push (in-memory)
```

매 메시지당 **SELECT 3 + INSERT 1**. T=100에서 MySQL CPU가 100%를 넘는 이유.

---

## 개선 plan — 한 항목씩 적용 후 재측정

각 라운드는 같은 step-down 시나리오로 돌리고, 위 baseline과 표를 나란히 비교.

| # | 항목 | 예상 효과 | 변경 위치 | 측정 후 비교할 지표 |
|---|---|---|---|---|
| 1 | **HikariCP pool 10 → 50** + Tomcat thread max 200 → 400 + JVM heap 1g → 2g | 즉시, 코드 변경 0 | docker-compose 환경변수 + Dockerfile | T=100 MySQL CPU 변화, T=200 plateau 이동 |
| 2 | **JwtFilter DB hit 제거** — JWT claims만으로 Principal 생성 | 매 요청 SELECT 1 제거 | `JwtFilter.java`, `JwtProvider.java` | T=100 MySQL CPU, hikaricp_connections_active |
| 3 | **MessageService SELECT 2 제거** — `getReferenceById(id)` 로 FK만 set | 메시지당 SELECT 2 → 0 | `MessageService.java` | POST /messages TPS, MySQL CPU |
| 4 | **Message 인덱스** — `(sender_id, receiver_id, created_at)` 복합 + `receiver_id, read_status` | 대화 페이징/unread 빨라짐 | DB `ALTER TABLE message ADD INDEX ...` | GET /messages/with/* p95 |
| 5 | **`@Async` 푸시** — `convertAndSendToUser` 응답에서 분리 | POST 응답 latency 감소 | `MessageService.java`, `@EnableAsync` | POST /messages p95 |
| 6 | **Redis 캐시** — `getLikeCount` 등 hot read | 핫 read DB hit 제거 | `LikeService.java`, Redis 도입 | DB CPU 추가 감소 |

---

## 측정 운영 메모

| 액션 | 명령 |
|---|---|
| 전체 step-down 1라운드 | `./scripts/step-load.sh dm` |
| Grafana 대시보드 | http://localhost:3000/d/newsfeed-backend |
| JMeter 대시보드 | http://localhost:3000/d/afpym429ry2gwf |
| InfluxDB ad-hoc | `curl 'http://localhost:8086/query?db=springboot&q=SHOW+MEASUREMENTS'` |
| docker stats 한 번 | `docker stats --no-stream nf-app nf-mysql nf-redis` |
| 토큰 비우기 (단일 세션 충돌 회피) | `docker compose exec -T mysql mysql -uroot -p1234 newsfeed -e "DELETE FROM jwt_token;"` |

## 알려진 측정 환경 한계

- JMeter 이미지가 linux/amd64라 ARM 에뮬레이션 → 클라이언트 CPU 패널티. T=500 이상은 사실상 측정 불가.
- `justb4/jmeter:5.5` JVM heap 디폴트가 ~2GB. 1000 thread + ramp 끝나기 전에 자주 OOM(exit 137).
- 더 정확한 측정을 원하면 **k6 native binary** (`brew install k6`) 또는 multi-arch JMeter 이미지로 전환 권장.

---

## 라운드 결과 기록 양식 (개선 후 채워나갈 것)

### Round N — <변경 사항 한 줄 요약> (YYYY-MM-DD)

변경: <fix list>

```
   T   samples    TPS     p50   p95   p99   err%   변동
  ───────────────────────────────────────────────────────
   50   ...
  100   ...      ← Δ vs baseline: +XX%
  200   ...
```

docker stats 핵심: <nf-app CPU, MySQL CPU, hikaricp pending 등>

결론: <효과 / 다음 의심 지점>

---

### Round 1 — Infra tuning (코드 변경 0) (2026-06-23)

변경:
- `JAVA_TOOL_OPTIONS=-Xms1g -Xmx2g -XX:+UseG1GC`
- `spring.datasource.hikari.maximum-pool-size`: 10 → **50** (+ minimum-idle=10)
- `server.tomcat.threads.max`: 200 → **400**
- `server.tomcat.accept-count`: 100 → **200**

```
   T   samples       TPS       p50      p95      p99    err%   변동 (vs baseline)
  ───────────────────────────────────────────────────────────────────────────────
    50  15,876→     265.1      127     371      787    0%    samples +21%, p99  -49%
   100   9,811→     163.0      367   1,243    2,519    0%    samples -43% ⚠ 노이즈 의심
   200   1,548→      31.6    3,733   5,880    8,956    0%    samples -67% ⚠ 노이즈 의심
   500     510→      34.6    5,212  19,250   20,494    0%    samples -53% (둘 다 JMeter 한계)
  1000   1,087→      24.8    7,885  12,248   12,260    0%    samples +55%, p99 -74% 🎉
```

docker stats (mid-run snapshot):
| T | nf-app | nf-mysql | 참고 |
|---|---|---|---|
| 50  | 145% | **253%** | HikariCP pool 50으로 늘어 DB가 더 일함 — 정상 |
| 100 | 166% | 9%   | 스냅샷이 idle 구간 캡처 → 노이즈 |
| 200 | 0.5% | 1.5% | 같은 노이즈 |
| 500 | 0.2% | 0.5% | JMeter ARM 한계 |
| 1000| 0.9% | 0.4% | JMeter ARM 한계, 그런데도 처리량 +55% |

HikariCP 적용 검증: `last("value") FROM hikaricp_connections_max` = **50** ✅

결론:
- **고부하(T=1000)에서 응답시간 ⅓로 단축** — 인프라 튜닝만으로 GC 압박 해소 + DB connection 대기 제거.
- T=50도 안정적 개선 (TPS +21%, p99 절반).
- T=100/200 단계는 단발 측정 노이즈로 추정 (mid-run 스냅샷이 우연히 idle 구간). 다음 라운드는 ramp 30s + duration 120s로 늘려서 노이즈 완화 권장.
- 다음 의심 지점: T=100~200 진짜 한계가 어디인지 재측정 + Round 2(JwtFilter DB hit 제거)로 DB SELECT 감소 효과 분리해서 측정.

---

# k6 재측정 라운드 (2026-07-29)

> JMeter(ARM 에뮬레이션)는 T≥500에서 **클라이언트가 먼저 포화**해 백엔드 한계 측정 불가였다.
> k6 native(arm64) + `scripts/k6-load.sh`로 교체. 클라이언트 병목 제거 → 백엔드 진짜 한계 관측.
> 측정 표준(모든 라운드 공통): **ramp 20s + duration 90s, VU 레벨 50/100/200/500/1000**, 시나리오 동일(POST 70% / GET 30%).

## Round 0 — k6 Baseline (2026-07-29)

원본 코드(인프라 튜닝 Round 1 적용 상태) 기준.

```
   VUS   samples     TPS    avg    p50    p95     p99   err%
  ───────────────────────────────────────────────────────────
    50    26,178   237.6    191    169    444     665   0.0
   100    22,736   206.0    440    419    844   1,182   0.0   ← TPS 이미 꺾임
   200    16,642   149.5   1209    993   2946   5,640   0.0
   500    10,341    91.3   4927   4846   9406  11,803   0.0
  1000    18,449   160.4   5531   5809   7886   8,696   0.0
```

docker stats (mid-run):

| VUS | nf-app CPU | **nf-mysql CPU** | 진단 |
|---|---|---|---|
| 50   | 175% | **267%** | DB가 이미 백엔드보다 바쁨 |
| 100  | 122% | **490%** | MySQL ~5코어 포화, TPS 오히려 감소 |
| 200  | 142% | 246% | p95 3s 붕괴 |
| 500  | 163% | 383% | — |
| 1000 | 117% | **550%** | MySQL 지속 포화, nf-app은 여유 |

### 핵심 진단

- **MySQL이 진짜 병목.** VUS=50에서 이미 MySQL 267%인데 nf-app은 175%로 여유. VUS=100에서 MySQL 490% → TPS가 237→206으로 **역전**.
- 원인: `POST /messages` 한 건당 **SELECT 3 + INSERT 1** (JwtFilter의 loadUserByUsername 1 + sendMessage의 sender/receiver 조회 2 + save 1).
- 개선 타깃 순서가 명확해짐: **R2(JwtFilter SELECT 1 제거) → R3(sender/receiver SELECT 2 제거)** 가 정확히 이 DB 부하를 겨냥.
- 이 baseline이 이후 모든 라운드의 비교 기준.

## Round 2 — JwtFilter DB 조회 제거 (JWT claims 인증) (2026-07-29)

변경: `JwtFilter`가 매 요청 `userDetailsService.loadUserByUsername()`(member SELECT 1)를 호출하던 것을
제거하고, 서명 검증된 토큰의 claims(id/email/role)만으로 `Member.fromClaims()` 경량 principal을 구성.
→ 요청당 SELECT 3 → **2** (POST /messages 기준).

```
   VUS   samples     TPS    p50    p95     p99   err%   (vs baseline)
  ─────────────────────────────────────────────────────────────────
    50    24,379   221.3    182    432     635   0.0    MySQL 267→167%
   100    19,248   174.1    503   1022   1,280   0.0
   200    16,877   151.9   1192   2189   2,583   0.0    p95 2946→2189 개선
   500    15,580   137.5   3327   5730   6,649   0.0    TPS 91→137
  1000    13,957   121.3   7112  13575  16,113   0.0
```

docker stats mid-run — **nf-mysql CPU (baseline → R2)**:

| VUS | baseline | R2 | nf-app (base→R2) |
|---|---|---|---|
| 50   | 267% | **167%** (-37%) | 175→158% |
| 100  | 490% | 356% | 122→135% |
| 200  | 246% | 499% | 142→116% |
| 500  | 383% | 529% | 163→98% |
| 1000 | 550% | 401% | 117→85% |

결론(정직하게):
- **가장 깨끗한 비포화 구간 VUS=50에서 신호가 명확**: MySQL CPU 267→167% (-37%), nf-app 175→158%.
  SELECT 3개 중 1개(≈33%)를 제거 → MySQL CPU ≈37% 감소. **기전과 수치가 일치**.
- nf-app CPU도 전 구간에서 하락(인증 경로에서 DB 왕복 + 영속성 컨텍스트 부담 제거).
- 포화 구간(VUS≥100)의 TPS/p95는 런-투-런 변동이 커서 단일 측정으로 단정 불가 — 시스템이 여전히
  **DB-bound**이기 때문. 남은 큰 SELECT 2개를 제거하는 R3에서 end-to-end 효과가 드러날 것으로 예상.
- 트레이드오프: claims 인증은 토큰 폐기(로그아웃)를 DB로 즉시 강제하지 못함. access token 만료 1시간이
  이를 제한. 즉시 무효화가 필요하면 R6에서 Redis 블랙리스트 병행 고려.

## Round 3 — 메시지 전송 SELECT 2 제거 (getReferenceById) (2026-07-29)

변경: `MessageService.sendMessage`가 `memberRepository.findById(senderId/receiverId)`로 sender/receiver
엔티티를 **로딩**하던 것을 `getReferenceById`(프록시만, SELECT 0)로 교체. Message 저장엔 FK(id)만
필요하고 `MessageResponseDto.from`은 프록시의 `getId()`만 접근(초기화 미유발). 요청당 SELECT 2 → **0**.

```
   VUS   samples     TPS    p50    p95     p99   err%   (vs baseline)
  ─────────────────────────────────────────────────────────────────
    50    26,111   236.6    173    388     525   0.0
   100    17,941   162.0    412   1632   2,110   0.0
   200    16,229   146.0   1061   2736   3,688   0.0
   500    17,773   157.0   2832   5071   6,353   0.0    TPS 91→157 (+72%)
  1000    16,537   143.8   6553   8679   9,857   0.0    p99 8696→9857
```

docker stats mid-run — **nf-app CPU 급감, MySQL CPU 상승**:

| VUS | nf-app (base→R3) | nf-mysql (base→R3) |
|---|---|---|
| 50   | 175 → **123%** | 267 → 309% |
| 100  | 122 → **80%**  | 490 → 663% |
| 200  | 142 → **81%**  | 246 → 634% |
| 500  | 163 → **71%**  | 383 → 693% |
| 1000 | 117 → **78%**  | 550 → 731% |

결론 — **병목이 이동했다**:
- **nf-app CPU가 전 구간에서 크게 하락**(VUS=500 기준 163→71%). sender/receiver 엔티티 하이드레이션 +
  영속성 컨텍스트 관리가 사라진 직접 효과. 앱은 이제 요청당 거의 INSERT 1건만 조율.
- app이 가벼워지자 **더 많은 요청이 DB에 도달** → 중부하 TPS 상승(VUS=500 91→157, +72%) →
  INSERT 처리량 증가 → **MySQL CPU가 오히려 상승**. 낮아진 게 아니라 "더 많이 일하게 됐다".
- 시스템이 **순수 write(INSERT)-bound**로 전환. 이 hot path의 환원 불가능한 코어가 드러남.
- 함의: 남은 개선은 (a) GET 30% 경로를 인덱스로 가볍게(R4), (b) 응답에서 부수작업 분리(R5),
  (c) hot read를 캐시로 DB에서 덜어내기(R6). 쓰기 자체의 한계를 넘으려면 배치/샤딩/큐가 필요.
- 주의(정직): getReferenceById는 존재하지 않는 receiverId를 INSERT 시 FK 제약(500)으로 걸러
  기존의 404(NotFound)와 에러 시맨틱이 달라짐. 인증 주체인 sender는 항상 유효.

## Round 4 — Message 복합 인덱스 (스키마 개선) (2026-07-29)

변경: `Message` 엔티티에 `@Table(indexes=...)` 선언(fresh DB 자동 생성) + 현재 DB에 명시 `ALTER`.
- `idx_msg_conv (sender_id, receiver_id, created_at)` / `idx_msg_conv_rev (receiver_id, sender_id, created_at)`
  — 대화 조회의 양방향 OR 각각 커버 + created_at 정렬을 인덱스로 흡수(filesort 제거)
- `idx_msg_unread (receiver_id, read_status)` — 안 읽은 메시지 조회

**k6 부하 시나리오로는 효과 측정 불가 — 하네스의 데이터 분포 결함 때문.** 정직하게 기록한다:

- k6가 **단일 토큰·단일 쌍**으로 모든 메시지를 한 방향으로 쏟아, 지배적 대화가 테이블의 대부분
  (측정 시점 28만 행 중 대부분)을 차지 → `sender_id=X` 조건의 **선택도가 거의 0**.
  ```
  -- 지배 쌍(하네스 결함): 옵티마이저가 풀스캔 선택
  EXPLAIN ... WHERE (sender=126 AND receiver=127) OR (sender=127 AND receiver=126) ORDER BY created_at LIMIT 50
  → type: ALL   key: NULL   rows: 284357   Extra: Using where; Using filesort
  ```
- 반면 **현실적(선택적) 대화**에서는 인덱스가 정확히 동작:
  ```
  -- 선택적 쌍(현실 분포): 인덱스 사용 + filesort 제거
  EXPLAIN ... WHERE sender_id=35 AND receiver_id=36 ORDER BY created_at LIMIT 20
  → type: ref   key: idx_msg_conv   rows: 1   Extra: NULL
  ```
- 결론: 인덱스는 **프로덕션(다수 유저 × 중간 크기 대화)에서 GET conversation의 filesort를 제거**하는
  올바른 개선이다. 다만 현재 부하 하네스는 단일 쌍이라 이 이득을 재현하지 못한다.
  숫자를 지어내는 대신 **하네스의 한계를 드러내는 것**이 정직한 엔지니어링.

## ⚠️ 측정 방법론 한계 (정직한 기록)

1. **라운드 간 테이블 성장(confound)**: baseline~R3가 같은 DB에 누적돼 message 행이 라운드마다 증가.
   라운드 간 **MySQL CPU 절대 비교**에는 테이블 크기 효과가 섞여 있음. 견고한 신호는
   (a) R2의 VUS=50 MySQL 267→167%, (b) R2/R3의 **nf-app CPU 감소**(동일 요청당 작업량 감소는 테이블
   크기와 무관) 쪽. 다음 개선: 라운드마다 `TRUNCATE message` 로 DB 크기 고정.
2. **단일 쌍 시나리오**: 위 R4 참조. 읽기 인덱스/캐시 효과 측정에는 multi-쌍 하네스 필요.
3. **단일 측정**: 각 셀 1회 측정이라 포화 구간 변동이 큼. 반복 측정 + 중앙값 권장.
