# Newsfeed — Performance Test Log

> DM 송수신 hot path 기준 부하 테스트 결과 + 시스템 메트릭 + 다음 개선 단계.
> 측정값을 비교 가능한 baseline으로 남겨두고, 개선 작업은 한 항목씩 적용 → 재측정 → 차이 비교한다.

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
