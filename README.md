# 🎈 NewsFeed 🎈

> **소셜 피드 플랫폼 프로젝트**  
> JWT(claims) 인증, 실시간 DM(STOMP/WebSocket), 카카오 로그인을 포함한 인스타그램형 SNS 서비스

## 📌 프로젝트 개요

**NewsFeed**는 사용자들이 사진 피드를 올리고 공유하는 인스타그램형 소셜 미디어 플랫폼입니다.
다중 이미지 게시글·팔로우 기반 피드·좋아요·댓글/대댓글·저장·검색·탐색·팔로우 추천에
**1:1 실시간 DM(사진·읽음)**과 **실시간 알림**, 차단/신고/비공개까지 제공합니다.
카카오 소셜 로그인과 JWT(claims 기반 stateless) 인증을 구현했으며, 프런트는 바닐라 JS SPA로
홈·상세·프로필·친구·DM·탐색·검색·저장·알림 화면을 모두 포함합니다.

## 🛠️ 기술 스택

- **Backend**: Java 17, Spring Boot 3.4, Spring Security, JPA/Hibernate
- **Database**: MySQL 8 (비즈니스 데이터·메시지 영속화), Redis 7
- **Authentication**: JWT — MVP2에서 **claims 기반 stateless 인증**으로 최적화(요청당 DB 조회 제거) + Kakao OAuth2
- **Real-time DM**: STOMP over WebSocket, **인메모리 SimpleBroker** + `convertAndSendToUser`, JWT 핸드셰이크 인증
- **Frontend**: 바닐라 JS SPA — 해시 라우터, 무한 스크롤, 이미지 업로드, 로그인/피드/상세/프로필/친구/DM 화면
- **Infra**: Docker Compose (app/MySQL/Redis/InfluxDB/Grafana)
- **Load Testing / Observability**: k6 native, Micrometer → InfluxDB → Grafana

> ℹ️ **Redis 사용 범위**: 현재 실시간 DM 브로커는 **단일 인스턴스 인메모리 SimpleBroker**입니다.
> Redis pub/sub 리스너(`chat` 채널)와 `RedisTemplate`는 **다중 인스턴스 fan-out 확장을 위한 스캐폴딩**으로만
> 배선돼 있고(`ChatMessageSubscriber`는 placeholder), 세션 저장소는 JDBC(MySQL)를 사용합니다.
> 스케일아웃 시 `enableStompBrokerRelay` 또는 Redis 발행/구독 연결이 다음 확장 포인트입니다.

## 🥅 개발 기간
- **초기 팀 프로젝트**: 2024/12/16 ~ 2024/12/31
- **MVP2 개인 확장**(프런트 SPA · Docker화 · 성능 최적화): 2026/06 ~

> **MVP2에서 추가된 것**: 바닐라 JS SPA 프런트엔드, Docker Compose 전체 스택(app/MySQL/Redis/InfluxDB/Grafana),
> k6 기반 부하테스트 + Grafana 모니터링, 그리고 **DM hot path 성능 최적화 케이스 스터디**(아래).

---

## 🚀 실행 방법 (Docker Compose)

```bash
# 1. 전체 스택 기동 (app 8080, MySQL 3307, Redis 6380, InfluxDB 8086, Grafana 3000)
docker compose up -d --build

# 2. 헬스체크
docker compose ps
curl -s -o /dev/null -w "GET / -> %{http_code}\n" http://localhost:8080/

# 3. 브라우저에서 SPA 접속
open http://localhost:8080/

# 4. API 회귀 테스트 (47→52 케이스)
./scripts/e2e.sh

# 5. 부하 테스트 (k6 native 필요: brew install k6)
DUR_S=90 RAMP_S=20 ./scripts/k6-load.sh baseline
```

- Grafana 대시보드: http://localhost:3000 (백엔드 Micrometer + k6 메트릭)
- 카카오 키 등 시크릿은 호스트 `.env`에서 주입 (`.env`는 커밋 제외)

---

## 📊 성능 최적화 케이스 스터디 (하이라이트)

DM 전송 hot path를 k6로 프로파일링해 **DB read-bound → write-bound로 병목을 이동**시키고,
요청당 DB SELECT를 **3 → 0**으로 줄였다.

| 라운드 | 변경 | 요청당 SELECT | 핵심 결과 |
|---|---|---|---|
| Baseline | 인프라 튜닝만 | 3 | MySQL 병목 (VUS=50 MySQL CPU **267%**) |
| **R2** | JwtFilter DB 조회 제거 (JWT claims 인증) | 3→2 | VUS=50 MySQL **267→167%** (-37%) |
| **R3** | 메시지 sender/receiver 조회 제거 (getReferenceById) | 2→**0** | nf-app CPU **급감**(VUS=500 163→71%), 중부하 TPS **+72%** |
| **R4** | 대화/안읽음 복합 인덱스 | — | 선택적 대화 filesort 제거 (EXPLAIN) |

측정 도구를 JMeter(ARM 에뮬레이션 한계)에서 **k6 native**로 교체해 클라이언트 병목을 제거했고,
하네스의 데이터 분포 결함·라운드 간 교란변수까지 문서화했다.

📄 **상세**: [loadtest/PERFORMANCE.md](./loadtest/PERFORMANCE.md)

---

## 🗓️ 진행 현황 (2026-08-26~27 세션 — 인스타형 대규모 확장)

**소셜 기능을 인스타그램 수준으로 확장** (스토리/릴스 제외). 두 계정 브라우저 E2E로 검증.

- **콘텐츠**: 다중 이미지(캐러셀·스와이프), 게시물 이미지 편집, 텍스트 전용 게시글, 탐색(Explore), 게시물/해시태그 검색
- **피드**: 팔로우 기반 홈 피드(팔로잉/최신/인기 3탭)
- **소셜**: per-user 좋아요(+댓글 좋아요·좋아요한 사람 목록), 저장(북마크)/공유, @멘션·#해시태그, 팔로워/팔로잉·팔로우 추천
- **실시간**: 알림(좋아요/댓글/답글/친구) STOMP 푸시 + 🔔 뱃지, DM 사진 전송·읽음 실시간 표시, 대화 삭제·안읽음 정렬
- **안전**: 차단(피드 필터), 신고, 비공개 계정
- **버그 수정**: 한글 IME Enter 중복 전송, 댓글 삭제 404, 만료 토큰 401·이중 POST 등

> 상세는 [TROUBLESHOOTING.md](./TROUBLESHOOTING.md) 참고. (PR #38·#39·#40)

## 🗓️ 진행 현황 (2026-08-25 세션)

**두 계정(Alice/Bob) 브라우저 E2E 검증 → 발견 버그 10건(B1~B10) 수정·재검증**

- **B3(Critical)** 친구 목록이 Friend row PK·sender 이름을 반환 → DM이 엉뚱한 회원에게 전송되던 버그 수정(상대 memberId·실명 반환) → DM 실제 전달 확인
- **B1/B2** 위치를 진짜 선택 항목으로(미입력도 게시), 프로필 회원 피드 조회 `@PathVariable` 누락 복구
- **B4** 받은/보낸 친구요청이 상대(발신/수신자) 정보를 표시하도록 정정
- **B8/B9** 댓글 작성자 실명 표시 + 대댓글(답글) UI 추가
- **B10** `/members/{id}`에 name/email/image 노출(DM·프로필 실명)
- **UI** 게시글 상세 사진:댓글 **6:4** 비율, DM 첫 메시지 헤더 가림 수정, 컨테이너 폭 확대
- **B7** 컨테이너 `TZ=Asia/Seoul`로 DM 시간 9시간 오차 해결

> 상세(증상→근본원인→수정)는 [TROUBLESHOOTING.md](./TROUBLESHOOTING.md) 2026-08-25 섹션 참고.

## 🗓️ 진행 현황 (2026-07-29 세션)

**완료**
- 6주치 미커밋 작업을 논리 단위 10개 커밋으로 정리 + `.gitignore` 정비(시크릿/산출물 제외)
- 버그 fix: 좋아요 0개 피드 조회 NPE(500→200), 피드 상세 응답에 `author`/`feedId` 포함(프런트 중복 호출 제거)
- 측정 하네스를 JMeter → **k6 native**로 재구축 (`scripts/k6-load.sh`, `loadtest/k6/dm.js`)
- 성능 라운드 측정·문서화: **Baseline / R2(claims 인증) / R3(getReferenceById) / R4(인덱스)**
- 케이스 스터디 종합 + 측정 방법론 한계까지 기록
- README·문서 정리, `docker compose up` 재현성 + e2e 52/52 확인

**남은 일**: [TODO.md](./TODO.md) 참고 (데모 영상, multi-쌍 하네스, R5/R6, 제품 화면 확장 등)

## 👨‍💻 ERD
*ERD 다이어그램 추가 예정*

## 👨‍💻 API 문서
*API 문서 링크 추가 예정*

## 🎯 주요 기능

### 👤 회원 관리 (Member)
- **회원가입/로그인**: PasswordEncoder를 활용한 비밀번호 암호화
- **JWT 토큰 발급**: 로그인 시 JWT 토큰 자동 발급
- **회원 정보 수정**: 비밀번호 확인 후 정보 수정 가능
- **카카오 소셜 로그인**: 카카오 OAuth2를 통한 간편 회원가입/로그인
- **회원 탈퇴**: SoftDelete 방식의 안전한 회원 삭제
- **비밀번호 변경**: 기존 비밀번호 검증 후 새 비밀번호 설정

### 📝 피드 관리 (Feed)
- **피드 작성**: **다중 이미지**(캐러셀) + 제목/문구, 텍스트 전용 게시글도 가능, **위치는 선택 항목**(입력 시 카카오 지오코딩으로 좌표 보완)
- **피드 정렬 3탭**: **팔로잉**(친구 글, 기본) · 최신 · 인기, 무한 스크롤
- **탐색(Explore)**: 인기 게시물 3×N 그리드 둘러보기(`/feeds/likecount`)
- **게시물 검색**: 제목/내용/#해시태그 부분일치(`/feeds/search`)
- **피드 수정/삭제**: 작성자만 — 수정 시 **이미지 교체/추가/삭제**까지 편집
- **프로필 피드**: 회원별 게시물 그리드(`/feeds/members/{id}`), 비공개 계정은 친구만
- **상세 캐러셀**: 다중 이미지 좌우 화살표/점/터치·마우스 스와이프

### 👥 친구/팔로우 시스템 (Friend)
- **친구 찾기/신청**: 이름·이메일 검색 후 친구 요청, **4탭 UI**(받은/보낸/목록/찾기)
- **팔로워/팔로잉**: 프로필에 게시물·팔로워·팔로잉 수 + 목록 모달
- **팔로우 추천**: '알 수도 있는 사람'(친구의 친구, 공통 친구 수 순) — 친구 탭·홈 상단
- **프로필 액션**: 타인 프로필에서 친구요청/끊기 · **메시지** · **차단** · **신고**
- **DM 연동**: 친구가 DM 대화 상대(상대 memberId·실명 반환)

### ❤️ 좋아요 시스템 (Like)
- **per-user 좋아요**: 회원당 1행 — 새로고침·기기 간 하트 상태 정확(`likedByMe`)
- **좋아요한 사람 목록**: 좋아요 수 클릭 → 회원 목록 모달
- **댓글 좋아요**: 댓글에도 ♥ (per-user)

### 💬 댓글 시스템 (Comment)
- **댓글/대댓글**: 작성자 실명 표시, 1단계 답글 인라인 입력
- **댓글 좋아요·삭제**: 본인 댓글 삭제, 각 댓글 ♥
- **@멘션·#해시태그**: 파란 링크(클릭 시 검색)

### 🔖 저장/공유 (Bookmark)
- **게시물 저장**: 🔖 북마크 토글 + 저장한 게시물 목록(`/bookmarks`)
- **공유**: 게시물 링크 복사

### 🔔 알림 (Notification)
- **실시간 알림**: 좋아요/댓글/답글/친구요청/수락 → STOMP로 즉시 도착, 🔔 뱃지·목록
- **읽음 처리**: 알림 열람 시 일괄 읽음

### 🔐 인증/인가 시스템 (Auth)
- **JWT(claims) stateless 인증**: 토큰 claims로 사용자/권한 판별(요청당 DB 조회 없음)
- **Access/Refresh 토큰**: 만료 시 refresh로 재발급(만료 시 401 → 자동 재발급), 만료 토큰 선제 갱신
- **카카오 OAuth2**: authorize 리다이렉트 + code 콜백 로그인

### 💬 실시간 DM (Message)
- **친구 기반 1:1 대화**: 친구 목록에서 상대 선택 → 대화방
- **STOMP/WebSocket**: 인메모리 SimpleBroker + `convertAndSendToUser` 실시간 푸시
- **사진 전송 · 읽음 표시**: 이미지 첨부, 상대가 읽으면 '읽음' **실시간** 갱신
- **대화 관리**: 대화 삭제, 안읽은 대화 상단 정렬 + 미읽음 점, 마지막 메시지 미리보기
- **한글 IME 대응**: 조합 확정 Enter 중복 전송 방지

### 🛡️ 안전/프라이버시
- **차단**: 회원 차단 → 팔로잉 피드에서 차단 회원 글 제외
- **신고**: 게시물/회원 신고 접수
- **비공개 계정**: 전환 시 친구만 프로필 게시물 조회 가능

## 🔧 기술적 특징

### 📡 JWT(claims) 인증
- **stateless 인증**: JWT claims에서 사용자/권한을 읽어 **요청당 DB 조회 제거**(MVP2 성능 최적화, R2 참고)
- **STOMP 핸드셰이크 인증**: WebSocket 업그레이드 시 `?token=` 쿼리로 JWT 검증
- **카카오 토큰 변환**: 카카오 AccessToken을 애플리케이션 JWT로 변환

### 🚀 실시간 통신
- **WebSocket vs SSE**: 양방향이 필요한 DM에 WebSocket(STOMP) 선택
- **유저 큐 3종**: `/queue/messages`(DM) · `/queue/notifications`(알림) · `/queue/read`(읽음) 을 `convertAndSendToUser`로 푸시
- **인메모리 SimpleBroker**: 단일 인스턴스 구조. 다중 인스턴스 시 Redis pub/sub fan-out 또는 STOMP relay(스캐폴딩 배선 완료)

### 💾 데이터베이스 설계
- **MySQL**: 회원/피드/댓글/좋아요/친구/**메시지** 등 모든 비즈니스 데이터·영속화
- **Redis**: 향후 캐시(R6 예정: 좋아요 hot-read)·pub/sub fan-out용으로 스택에 포함
- **Spring Session**: JDBC(MySQL) 저장소 사용

---

상세한 트러블슈팅 내용은 [TROUBLESHOOTING.md](./TROUBLESHOOTING.md)를 참고하세요.
