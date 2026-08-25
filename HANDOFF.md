# 다음 세션 인수인계 (2026-06-17 작성)

> 이 문서는 **다음 날 작업을 이어서 하기 위한 메모**다. 어디까지 끝났고, 어디부터 시작하면 되는지, 환경을 어떻게 다시 띄우는지 한 장에 정리.

---

## 0) 우선 환경 살리기

```bash
# 1. Docker Desktop이 꺼져 있으면
open -a Docker
# (자동으로 데몬 뜸. 잠시 기다리기)

# 2. 스택 부활
cd ~/IdeaProjects/newsfeed
docker compose up -d

# 3. 헬스체크
docker compose ps
curl -s -o /dev/null -w "GET / -> %{http_code}\n" http://localhost:8080/

# 4. 브라우저
open http://localhost:8080/
```

- 포트: app **8080**, MySQL **3307**(컨테이너 내부 3306), Redis **6380**
- MySQL 자격: `root` / `1234`, DB 이름 `newsfeed`
- 카카오 키는 호스트 `.env`에서 자동 주입 (`docker-compose.yml`의 `${KAKAO_CLIENT_ID:-dummy}` 패턴)
- 컨테이너 로그는 호스트 `./logs/app.log`에 그대로 쌓임 (`tail -f logs/app.log`)
- 업로드된 이미지는 `./uploads/yyyy/MM/uuid.ext`

---

## 1) 어제 어디까지 했나

### 추가/생성한 파일
- 프런트 SPA: `src/main/resources/static/{index.html, css/style.css, js/{api,auth,detail,feed,main,store,ui}.js}` + `dev/ws-test.html` (기존 STOMP 테스트 페이지 이동)
- 백엔드 신규: `auth/config/SecurityConfig.java` 보강, `constants/config/FileUploadConfig.java`, `file/controller/FileUploadController.java`, `message/service/ChatMessageSubscriber.java`
- 인프라: `Dockerfile`, `docker-compose.yml`, `.dockerignore`
- 검증: `scripts/e2e.sh` (PASS 47 / FAIL 0)
- 문서: `TROUBLESHOOTING.md` 끝에 17건 정리 (`## 🛠 mvp2/feed — 프런트엔드 추가 & Docker 전환 라운드 (2026-06-17)`)

### 사용자 기존 코드에서 손댄 부분(꼭 알아둘 것)
- `Member.messages` `mappedBy` → `sender` (JPA 매핑 정합)
- `FeedServiceImpl.updateFeed`, `deleteFeed`의 `if (existsById(...))` → `if (!existsById(...))`
- `JwtToken.accessToken/refreshToken`에 `@Column(length=1024)` (DB도 `ALTER` 적용)
- `MemberRequestDto.password`에 `@Length(min=8)` + `@Pattern` (영문·숫자·특수문자)
- `MemberController` GetMapping path에 `:[0-9]+` 정규식 제약
- `application.yml` `spring.config.import`에 `optional:` 추가, `spring.servlet.multipart` 추가
- `GlobalExceptionController`에 `@Slf4j` + 로그 출력
- import 경로 3건, `TokenRepository` 메서드 3개, `JwtProvider.getExpiration`, `UserDetailsImpl.getMember`, `AuthenticatedMemberUtil.getAuthenticatedMemberId` 모두 정의 추가

### 현재 동작하는 화면 / API
- **화면**: 로그인 / 회원가입 / 홈피드(인기순 무한 스크롤) / 디테일(작성자·좋아요·댓글·대댓글·작성)
- **API**: e2e가 47개 케이스 통과 — auth(login/refresh/reissue/logout), members(signup/profile/update/password/delete), files/image, feeds(create/get/byMember/likecount/update/delete), likes(like/dislike/count), comments(create/child/get/byFeed/update/delete), friends(request/accept/sent/received/list/delete)

---

## 2) 내일 할 일 (우선순위 순)

### 🔴 HIGH — 기능 정확성 / 데이터 일관성

1. **`LikeService.getLikeCount` NPE 정공법 fix**
   - 위치: `like/service/LikeServiceImpl.java` 또는 `LikeRepository`의 SUM 쿼리
   - 현상: 좋아요 0개인 피드에 `GET /likes/{feedId}` 시 `Cannot invoke "java.lang.Long.intValue()" because "count" is null`
   - 조치: JPQL에 `COALESCE(SUM(l.likeCount), 0L)` 또는 서비스에서 `count == null ? 0 : count.intValue()`
   - 끝나면 `e2e.sh`의 `GET like count` 호출 순서 원복 가능

2. **`AuthService` 단일 세션 강제 정책 결정**
   - 위치: `member/service/AuthService.java#login`의 `existsByMemberEmailNot(email)` 분기
   - 현상: 다른 누군가가 로그인 상태이면 새 로그인 전체 거부 (멀티유저 환경에서 모순)
   - 옵션 A: 분기 자체 제거 → 누구나 동시 로그인 가능
   - 옵션 B: 본인 토큰만 확인 → `existsByMemberEmail(email)`로 바꾸고 본인 기존 토큰만 정리
   - **사용자 의사 확인 필요**

3. **회원가입 시 `role` 필드 클라이언트 의존 제거**
   - 위치: `member/dto/MemberRequestDto.java`
   - 현상: `role`이 `@NotNull`이라 클라이언트가 매번 `"USER"` 보내야 함. 옛 캐시 페이지가 누락 시 가입 실패.
   - 조치: `@NotNull` 제거 + `toEntity()`/서비스에서 무조건 `Role.USER` 강제. (이미 `toEntity` 안에 있음)

4. **`LoginType` 기본값 처리**
   - 위치: `MemberRequestDto` 또는 `MemberServiceImpl.createMember`
   - 현상: 클라이언트가 안 보내면 null → KakaoMember가 아닌 경우 어떻게 처리할지 불명확
   - 조치: 일반 가입은 `LoginType.NORMAL_USER`로 강제 세팅

5. **`FeedResponseDto`/`FeedWithLikeCountDto`에 작성자 정보 + feedId 포함**
   - 현상: 현재 디테일 화면이 작성자를 알기 위해 `GET /members/{feedId}/member` 별도 호출 중. 그것도 `feedId/member` 같은 어색한 URL.
   - 조치: 응답에 `feedId`, `author: {id, name, image}` 포함. 프런트는 한 번에 모든 정보 받음.

6. **`FriendResponseDto`에 Friend 엔티티 PK 포함**
   - 현상: 클라이언트가 친구 삭제하려면 `DELETE /friends/{friendId}`인데, 응답에는 senderId만 있어서 PK 알 수가 없음 (e2e는 DB 직접 조회로 우회 중)
   - 조치: `FriendResponseDto`에 `id` 필드 추가

### 🟡 MEDIUM — 화면 확장

7. **프로필 화면** (`#/profile/:memberId` 추가)
   - 인스타식 3×N 그리드 (`GET /feeds/members/{memberId}` 활용)
   - 헤더에 닉네임/팔로워 수/팔로잉 수
   - `js/profile.js` 신규, `main.js` 라우터에 매칭 추가

8. **친구 화면** (`#/friends`)
   - 보낸 요청 / 받은 요청 / 친구 목록 3탭
   - 받은 요청에 수락 버튼
   - `GET /friends/sent|received|`, `PATCH /friends/accept`, `DELETE /friends/{friendId}`

9. **카카오 로그인 버튼**
   - 로그인 페이지에 "카카오로 시작하기" 버튼
   - 카카오 JS SDK로 access_token 받은 뒤 `POST /kakao/login` (Bearer)
   - 백엔드는 이미 완비

10. **회원 프로필 이미지 렌더**
    - 현재 모든 아바타가 그라데이션 + 이니셜만
    - `Member.image` 있으면 그걸 `img`로 표시 (`avatar()` 헬퍼 보강)

11. **DM 화면** (`#/dm`, `#/dm/:peerId`)
    - 백엔드 STOMP 핸드셰이크에 JWT 검증 이미 있음 (`JwtHandShakeInterceptor`)
    - 옛 `dev/ws-test.html` 참고해서 SockJS+STOMP 클라이언트 작성
    - 친구 목록에서 "메시지" 버튼 → DM 진입

### 🟢 LOW — 정리/품질

12. **`.env`의 카카오 시크릿/JWT secret 회수**
    - 현재 평문 노출. git history에도 들어있을 가능성 (`git log -- .env`).
    - 시크릿 로테이션 → `.env.example`만 커밋, 실제 `.env`는 무시.

13. **`@ExceptionHandler(Exception.class)` catch-all 세분화**
    - 위치: `exception/GlobalExceptionController.java`
    - 현상: 모든 미처리 예외를 400으로 가림 → 디버깅 어려움
    - 조치: `DataIntegrityViolationException` 등은 그대로 5xx, 핸들러 종류별 분리

14. **favicon 추가** — `src/main/resources/static/favicon.ico` 1×1 PNG라도 넣으면 콘솔 400 사라짐

15. **JUnit 통합 테스트** — 현재 e2e는 셸 스크립트. CI에 묶으려면 `@SpringBootTest` + Testcontainers(MySQL/Redis) 권장

16. **README.md 업데이트** — Docker 실행법(`docker compose up -d`), 새 비번 룰, 프런트 URL을 한 섹션 추가

17. **응답 포맷 통일** — `Friend*`, `Kakao*` 컨트롤러는 `CommonResponse` 안 씀. 프런트의 api.js 언래핑 로직과 어긋남. 통일 권장.

18. **이미지 업로드 시 리사이즈** — 현재 원본 그대로 저장. 큰 파일 들어오면 부하. 1080×1080 이내로 리사이즈.

---

## 3) 알려진 회피책 / 임시 코드

- **e2e의 `db_truncate_tokens`**: 단일 세션 정책 우회용. 위 HIGH-2 결정되면 제거 가능.
- **e2e의 `GET like count` 호출 위치**: NPE 회피용. HIGH-1 끝나면 원위치 가능.
- **e2e의 `FRIEND_PK = MySQL 직접 SELECT`**: HIGH-6 끝나면 응답에서 추출로 변경.
- **`MemberRequestDto.toEntity`가 `Role.USER` 하드코딩**: HIGH-3 정리 시 이게 단일 진실의 원천이 되도록.

---

## 4) 실행 체크리스트

작업 시작 시:
- [ ] `docker compose ps` — 세 컨테이너 다 Up 인지
- [ ] `curl localhost:8080/` 200 인지
- [ ] `./scripts/e2e.sh` 한 번 돌려서 47/47 인지

작업 중:
- [ ] 백엔드 코드 변경 → `docker compose up -d --build app`
- [ ] 프런트 변경 → 재빌드 필요 없음. 브라우저 강제 새로고침(`Cmd+Shift+R`)
- [ ] DB 스키마 변경 필요 시 `docker compose exec -T mysql mysql -uroot -p1234 newsfeed -e "..."` 또는 엔티티만 바꾸고 `ddl-auto=update`에 맡김 (단, 컬럼 길이 축소는 update가 안 함 — 직접 ALTER)

작업 끝나기 전:
- [ ] `./scripts/e2e.sh` 다시 → 47/47 유지인지
- [ ] 새로 만든 화면이면 브라우저에서 실제 클릭 → 동작 확인
- [ ] `TROUBLESHOOTING.md` / 이 `HANDOFF.md` 갱신
- [ ] 커밋 (아직 한 번도 안 했음)

---

## 5) 미커밋 변경 사항 (2026-06-17 시점)

```
Modified:
  .env                                   # ddl-auto: create → update
  .gitignore                             # uploads/, logs/ 추가
  src/main/resources/application.yml     # multipart, optional .env import
  src/main/java/com/example/newsfeed/auth/config/SecurityConfig.java
  src/main/java/com/example/newsfeed/auth/jwt/entity/JwtToken.java
  src/main/java/com/example/newsfeed/auth/jwt/repository/TokenRepository.java
  src/main/java/com/example/newsfeed/auth/jwt/service/JwtProvider.java
  src/main/java/com/example/newsfeed/auth/jwt/service/UserDetailsImpl.java
  src/main/java/com/example/newsfeed/auth/jwt/dto/JwtMemberDto.java
  src/main/java/com/example/newsfeed/exception/GlobalExceptionController.java
  src/main/java/com/example/newsfeed/feed/dto/FeedWithLikeCountDto.java
  src/main/java/com/example/newsfeed/feed/repository/FeedRepository.java
  src/main/java/com/example/newsfeed/feed/service/FeedServiceImpl.java
  src/main/java/com/example/newsfeed/like/controller/LikeController.java
  src/main/java/com/example/newsfeed/member/controller/MemberController.java
  src/main/java/com/example/newsfeed/member/dto/MemberRequestDto.java
  src/main/java/com/example/newsfeed/member/entity/Member.java
  src/main/java/com/example/newsfeed/message/entity/Message.java
  src/main/java/com/example/newsfeed/util/AuthenticatedMemberUtil.java
  TROUBLESHOOTING.md

Added:
  Dockerfile
  docker-compose.yml
  .dockerignore
  HANDOFF.md (이 문서)
  scripts/e2e.sh
  src/main/java/com/example/newsfeed/constants/config/FileUploadConfig.java
  src/main/java/com/example/newsfeed/file/controller/FileUploadController.java
  src/main/java/com/example/newsfeed/message/service/ChatMessageSubscriber.java
  src/main/resources/static/{index.html, css/style.css, js/*.js, dev/ws-test.html}
  uploads/                               # gitignore됨
  logs/                                  # gitignore됨
```

커밋 전략 추천(논리 단위로 쪼개기):
1. 기존 코드 컴파일 수정 (import 경로, 누락 메서드)
2. JPA 매핑/스키마 fix (`Member.messages`, `JwtToken.accessToken length`)
3. `FeedServiceImpl` 부정 조건 버그 fix
4. SecurityFilterChain + Multipart + 파일 업로드 기능
5. 프런트엔드 SPA 추가
6. Docker 환경 (Dockerfile, compose, dockerignore)
7. 비번 룰 강화 + GET path 가드
8. e2e 스크립트 + 트러블슈팅/인수인계 문서

---

작업하다 새 문제 생기면 `TROUBLESHOOTING.md`에 추가, 계획이 바뀌면 이 문서 갱신.
