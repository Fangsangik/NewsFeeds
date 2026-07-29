# 🎈 NewsFeed 🎈

> **소셜 피드 플랫폼 프로젝트**  
> JWT + Session 하이브리드 인증, Redis 채팅, 카카오 로그인을 포함한 SNS 서비스

## 📌 프로젝트 개요

**NewsFeed**는 사용자들이 피드를 작성하고 공유할 수 있는 소셜 미디어 플랫폼입니다. 친구 시스템, 좋아요 기능, 댓글 시스템, 실시간 채팅 등을 제공하며, 카카오 소셜 로그인과 JWT + Session 하이브리드 인증 시스템을 구현했습니다.

## 🛠️ 기술 스택

- **Backend**: Java 17, Spring Boot 3.4, Spring Security, JPA/Hibernate
- **Database**: MySQL 8, Redis 7
- **Authentication**: JWT (MVP2에서 claims 기반 stateless 인증으로 최적화), Kakao OAuth2
- **Real-time**: STOMP over WebSocket (JWT 핸드셰이크 인증)
- **Frontend**: 바닐라 JS SPA (해시 라우터, 무한 스크롤)
- **Infra**: Docker Compose (app/MySQL/Redis/InfluxDB/Grafana)
- **Load Testing / Observability**: k6 native, Micrometer → InfluxDB → Grafana

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
하네스의 데이터 분포 결함·라운드 간 교란변수까지 정직하게 문서화했다.

📄 **상세**: [loadtest/PERFORMANCE.md](./loadtest/PERFORMANCE.md)

---

## 🗓️ 진행 현황 (2026-07-29 세션)

**완료**
- 6주치 미커밋 작업을 논리 단위 10개 커밋으로 정리 + `.gitignore` 정비(시크릿/산출물 제외)
- 버그 fix: 좋아요 0개 피드 조회 NPE(500→200), 피드 상세 응답에 `author`/`feedId` 포함(프런트 중복 호출 제거)
- 측정 하네스를 JMeter → **k6 native**로 재구축 (`scripts/k6-load.sh`, `loadtest/k6/dm.js`)
- 성능 라운드 측정·문서화: **Baseline / R2(claims 인증) / R3(getReferenceById) / R4(인덱스)**
- 케이스 스터디 종합 + 측정 방법론 한계까지 정직하게 기록
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
- **피드 작성**: 위치 정보 포함 피드 생성
- **피드 조회**: 개별/전체 피드 조회 기능
- **좋아요 순 정렬**: 인기 피드 우선 표시
- **피드 수정/삭제**: 작성자만 수정/삭제 가능
- **위치 기반 서비스**: 주소 입력 시 현재 위치 정보 표시

### 👥 친구 시스템 (Friend)
- **친구 신청**: 회원 ID를 통한 친구 요청
- **친구 요청 관리**: 받은 요청 확인 및 수락/거절
- **친구 목록**: 전체 친구 목록 조회
- **친구 삭제**: 친구 관계 해제 기능
- **보안 검증**: Session 기반 권한 확인

### ❤️ 좋아요 시스템 (Like)
- **좋아요 기능**: 피드에 대한 좋아요 표시
- **좋아요 취소**: 좋아요 해제 기능
- **좋아요 수 확인**: 실시간 좋아요 개수 표시

### 💬 댓글 시스템 (Comment)
- **댓글 작성**: 피드에 댓글 작성
- **대댓글 기능**: 계층형 댓글 구조
- **댓글 수정/삭제**: 작성자만 수정/삭제 가능

### 🔐 인증/인가 시스템 (Auth)
- **하이브리드 인증**: JWT + Session 조합 사용
- **Interceptor**: 요청 전/후 처리를 통한 인증 확인
- **카카오 OAuth2**: 외부 API 연동 소셜 로그인
- **토큰 관리**: JWT 토큰 생성, 검증, 만료 처리

### 💬 실시간 채팅 (Message)
- **WebSocket 통신**: 실시간 양방향 메시지 교환
- **Redis Pub/Sub**: 메시지 브로커로 Redis 활용
- **메시지 저장**: MySQL과 Redis 하이브리드 저장
- **읽음 처리**: 메시지 읽음 상태 관리

## 🔧 기술적 특징

### 📡 하이브리드 인증 시스템
- **JWT + Session**: 보안성과 편의성을 모두 고려한 인증 방식
- **Interceptor 활용**: Spring MVC 단계에서의 요청 처리
- **카카오 토큰 변환**: 카카오 AccessToken을 애플리케이션 JWT로 변환

### 🚀 실시간 통신
- **WebSocket vs SSE**: 양방향 통신이 필요한 채팅에 WebSocket 선택
- **Redis 활용**: 메모리 기반 빠른 데이터 처리
- **메시지 영속성**: Redis와 MySQL 조합으로 데이터 안정성 확보

### 💾 데이터베이스 설계
- **MySQL**: 주요 비즈니스 데이터 저장
- **Redis**: 세션, 캐시, 실시간 메시지 처리
- **Spring Session**: 세션 정보의 외부 저장소 관리

---

상세한 트러블슈팅 내용은 [TROUBLESHOOTING.md](./TROUBLESHOOTING.md)를 참고하세요.
