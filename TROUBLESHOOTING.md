# NewsFeed - 문제 해결 및 트러블슈팅 📝

이 문서는 NewsFeed 프로젝트 개발 과정에서 발생한 주요 문제들과 해결 방법을 정리한 문서입니다.

---

## 🧨 Trouble Shooting

### 👤 Member (회원 관리)

#### **문제점**: sender와 receiver의 mapping id 값 설정 오류
**해결방법**: Entity 관계 매핑을 정확히 수정하여 올바른 ID 값이 설정되도록 개선

---

### 👥 Friend (친구 시스템)

#### 1) **Session 사용자 검증 로직 부재**

**문제가 있었던 코드**:
```java
@Transactional
public FriendResponseDto addFriend(Long memberId, FriendRequestDto friendRequestDto) {
    Member sender = memberRepository.findById(friendRequestDto.getSenderId())
            .orElseThrow(() -> new IllegalArgumentException("Member does not exist"));

    Member receiver = memberRepository.findById(friendRequestDto.getReceiverId())
            .orElseThrow(() -> new IllegalArgumentException("Member does not exist"));

    boolean alreadyFriend = friendRepository.existsBySenderAndReceiver(sender, receiver)
            || friendRepository.existsBySenderAndReceiver(receiver, sender);

    if (alreadyFriend) {
        throw new IllegalArgumentException("Already friends");
    }

    Friend friend = FriendRequestDto.createFriend(sender, receiver);

    return FriendResponseDto.toDto(friendRepository.save(friend));
}
```

**개선된 코드**:
```java
public FriendResponseDto addFriend(Long memberId, FriendRequestDto friendRequestDto) {
    // 요청 발신자가 세션 사용자와 동일한지 확인
    if (!memberId.equals(friendRequestDto.getSenderId())) {
        throw new IllegalArgumentException("권한이 없습니다. 요청 발신자가 세션 사용자와 다릅니다.");
    }

    Member sender = memberRepository.findById(friendRequestDto.getSenderId())
            .orElseThrow(() -> new IllegalArgumentException("발신자 회원이 존재하지 않습니다."));

    Member receiver = memberRepository.findById(friendRequestDto.getReceiverId())
            .orElseThrow(() -> new IllegalArgumentException("수신자 회원이 존재하지 않습니다."));

    boolean alreadyFriend = friendRepository.existsBySenderAndReceiver(sender, receiver)
            || friendRepository.existsBySenderAndReceiver(receiver, sender);

    if (alreadyFriend) {
        throw new IllegalArgumentException("이미 친구 관계입니다.");
    }

    Friend friend = Friend.builder()
            .sender(sender)
            .receiver(receiver)
            .status(FriendRequestStatus.REQUESTED)
            .build();

    return FriendResponseDto.toDto(friendRepository.save(friend));
}
```

#### 2) **친구 수락시 Friend DB의 PK 값으로 수락이 되는 문제**

**문제점**: 친구 요청 수락 시 Friend Entity의 PK가 아닌 회원의 고유 ID로 요청을 처리해야 했음

**해결방법**: 회원의 실제 ID를 기반으로 친구 요청을 처리하도록 로직 수정

---

### 💬 Comment (댓글 시스템)

#### 1) **NPE (NullPointerException) 발생**

**문제가 있었던 코드**:
```java
@Getter
@Builder
public class CommentResponseDto {
    private Long commentId; 
    private String content; 
    private Long feedId; 
    private List<CommentResponseDto> childComments;

    public static CommentResponseDto toDto(Comment comment) {
        return CommentResponseDto.builder()
                .commentId(comment.getId())
                .content(comment.getContent())
                .feedId(comment.getFeed().getId())
                .build(); // childComments 변환 누락
    }
}
```

**해결한 코드**:
```java
@Getter
@Builder
public class CommentResponseDto {
    private Long commentId; 
    private String content; 
    private Long feedId; 
    private List<CommentResponseDto> childComments;

    public static CommentResponseDto toDto(Comment comment) {
        return CommentResponseDto.builder()
                .commentId(comment.getId())
                .content(comment.getContent())
                .feedId(comment.getFeed().getId())
                .childComments(comment.getChildren() != null ? 
                               comment.getChildren().stream()
                                      .map(CommentResponseDto::toDto)
                                      .toList() : new ArrayList<>()) // 대댓글 변환 추가
                .build();
    }
}
```

#### 2) **회원 정보 누락**

**문제된 코드**:
```java
Comment childComment = Comment.builder()
        .content(requestDto.getContent())
        .feed(parentComment.getFeed())
        .build();
```

**해결한 코드**:
```java
Comment childComment = Comment.builder()
        .content(requestDto.getContent())
        .feed(parentComment.getFeed())
        .member(member) // 작성자 정보 설정
        .build();
```

#### 3) **JSON 응답시 대댓글이 부모 댓글과 분리**

**문제 코드**:
```java
public List<CommentResponseDto> getCommentsByFeedId(Long feedId) {
    List<Comment> comments = commentRepository.findByFeedId(feedId);
    return comments.stream()
            .map(CommentResponseDto::toDto) // 모든 댓글을 1차원 리스트로 변환
            .toList();
}
```

**해결 코드**:
```java
public List<CommentResponseDto> getCommentsByFeedId(Long feedId) {
    List<Comment> comments = commentRepository.findByFeedId(feedId);

    return comments.stream()
            .filter(comment -> comment.getParent() == null) // 부모가 없는 댓글만 필터링
            .map(CommentResponseDto::toDto)
            .toList();
}
```

#### 4) **Lazy Loading 문제로 대댓글이 로드되지 않음**

**문제 코드**:
```java
@Query("SELECT c FROM Comment c WHERE c.feed.id = :feedId")
List<Comment> findByFeedId(@Param("feedId") Long feedId);
```

**해결 코드**:
```java
// Fetch join으로 해결
@Query("SELECT c FROM Comment c LEFT JOIN FETCH c.children WHERE c.feed.id = :feedId")
List<Comment> findByFeedId(@Param("feedId") Long feedId);
```

---

### 🍯 카카오톡 OAuth2

#### 1) **리다이렉트 URI에서 Access Token 발급 문제**

**문제점**: 인증 인가 받기 이후, 리다이렉트 URI에서 Access Token 발급으로 로그인 또는 가입 처리 후, 필요한 서비스 페이지로 이동이 되어야 했지만, 리다이렉트 URI에 머물러 사용자가 새로고침 할 수 있는 경우, 이미 사용된 인가 코드를 이용한 토큰 발급 요청을 다시 시도하게 되면 Error 발생

**원인**: Controller에서 토큰을 받을때 redirect를 직접 지정할 필요가 없었는데 구현해놓았기 때문에 두번 호출 문제 발생

**해결방법**: 불필요한 리다이렉트 로직 제거

#### 2) **Access Token 치환 문제**

**문제점**: Access Token으로 내가 작성한 Feeds나 다른 기타 기능들을 사용하려고 했는데 카카오에서 발송한 Access Token은 카카오톡 유저 정보 즉, 카카오로만 사용 가능한 토큰

**해결방법**: 카카오 토큰을 애플리케이션에 맞는 JWT 토큰으로 치환
```java
public ResponseEntity<CommonResponse<Map<String, String>>> kakaoLogin(@RequestHeader("Authorization") String kakaoAccessToken) {
    // 카카오 Access Token으로 사용자 정보 조회 후 JWT 생성
    KakaoUserInfoResponseDto userInfo = kakaoService.getUserInfo(kakaoAccessToken);
    Map<String, String> jwtTokens = kakaoService.createJwtFromKakaoUserInfo(userInfo);

    return ResponseEntity.ok(new CommonResponse<>("로그인 성공", jwtTokens));
}
```

#### 3) **Redirect_URI 설정 문제**

**문제점**: redirect_uri 설정이 잘 되어 있다고 생각했지만, HTTPS인지 HTTP인지 그리고 //가 두개가 들어가는지 하나가 들어가는지 정확하지 않았음

**해결방법**: 카카오 개발자 콘솔에서 정확한 URI 형식으로 설정

---

### 🔄 Interceptor (인터셉터)

#### 1) **회원 생성 시 인터셉터 차단 문제**

**문제점**: 회원을 생성하는데 인터셉터에서 회원 생성로직을 막아 생성이 되지 않는 문제

**원인**: HIGHEST_PRECEDENCE level 설정이 애매했고, excludePatterns 설정 부족

**해결방법**: excludePatterns을 사용해 interceptor가 적용되지 않는 부분을 따로 설정

#### 2) **인코딩 문제**

**문제점**: 인코딩 문제가 발생해 Message가 깨져 나왔음

**해결방법**: CharacterEncodingFilter 추가
```java
@Bean
public CharacterEncodingFilter characterEncodingFilter() {
    CharacterEncodingFilter filter = new CharacterEncodingFilter();
    filter.setEncoding("UTF-8");
    filter.setForceEncoding(true);
    return filter;
}
```

---

### 🔐 JWT (JSON Web Token)

#### 1) **Secret Key 리셋 문제**

**문제점**: 인텔리제이를 다시 시작할때 update로 설정해 놓았지만, 계속 secret Key가 reset되는 상황

**문제 코드**:
```java
public JwtProvider() {
    this.secretKey = Keys.secretKeyFor(SignatureAlgorithm.HS256);
}
```

**변경 코드**:
```java
public JwtProvider(@Value("${jwt.secret}") String secret) {
    this.secretKey = new SecretKeySpec(secret.getBytes(StandardCharsets.UTF_8), "HmacSHA256");
}
```

**해결방법**: yml에 JWT 고정 값 설정

---

### 📝 Feed (피드)

#### 1) **Hibernate 프록시 객체 직렬화 오류**

**문제점**:
```
Type definition error: [simple type, class org.hibernate.proxy.pojo.bytebuddy.ByteBuddyInterceptor]
```

**원인**: Hibernate가 Lazy 로딩 된 엔티티 프록시 객체를 JSON 직렬화 하려고 할때 발생

**해결방법**: Entity로 받는것이 아닌 DTO로 변환하여 반환

#### 2) **MultipleBagFetchException**

**문제점**:
```
org.hibernate.loader.MultipleBagFetchException: cannot simultaneously fetch multiple bags:
```

**원인**: OneToMany 관계에서 Entity 두개를 fetch join에 가져오는 것이 문제

**해결방법**: @JsonIgnore로 해결

#### 3) **피드 좋아요 내림차순 정렬시 좋아요 개수가 반영되지 않는 문제**

**원인**: 전체 개수를 가져오지 않아서 문제가 발생

**해결방법**: 좋아요 개수를 정확히 계산하여 반영하도록 쿼리 수정

---

### 🗄️ Session Error

**문제점**:
```
PreparedStatementCallback; bad SQL grammar [DELETE FROM SPRING_SESSION WHERE SESSION_ID = ? AND MAX_INACTIVE_INTERVAL >= 0 ]
```

**원인**: 스프링 Session table이 생성되지 않아서 뜨는 문제

**해결방법**: yml에 다음 설정 추가
```yaml
spring:
  session:
    store-type: ${SPRING_SESSION_STORE_TYPE}
    jdbc:
      initialize-schema: always
```

---

### 🔑 로그인 문제

#### 1) **JWT 토큰 불필요한 검증**

**문제점**: JWT 토큰 값이 필요할 것이라고 생각했지만, 로그인은 단순 아이디와 패스워드 비교하기 위함이지 토큰 값까지 넣을 필요 없었음

**해결방법**: WebConfig에서 whiteList url Pattern에 로그인 경로 추가

#### 2) **다중 계정 로그인 문제**

**문제점**: 로그아웃이 진행되지 않았는데 다른 계정으로 로그인 시도시 로그인이 되는 문제

**해결방법**: AuthService의 login 메서드에 중복 로그인 검증 로직 추가
```java
Optional<JwtToken> findByMemberId(Long memberId);
boolean existsByMemberEmailNot(String email);
```

#### 3) **로그아웃 처리 문제**

**문제점**: 로그아웃 처리시 제대로된 로그아웃이 진행이 안됨

**해결방법**: 해당 회원에 조회되는 토큰이 있는지 조회 후 해당 토큰을 삭제
```java
@Transactional
public void logout(Long memberId) {
    JwtToken jwtToken = tokenRepository.findByMemberId(memberId)
            .orElseThrow(() -> new IllegalArgumentException("Token does not exist"));
    tokenRepository.delete(jwtToken);
}
```

---

## 💡 확장 & 개선 사항

### MVP1에서의 성과
- 기본적인 SNS 기능 구현 완료
- JWT + Session 하이브리드 인증 시스템 구축
- 카카오 소셜 로그인 연동
- 실시간 채팅 기반 구조 마련

### MVP2 계획
- **테스트 코드**: 단위 테스트 및 통합 테스트 추가
- **Spring Security**: 현재 Interceptor + Session을 Security로 전환
- **Redis 완전 연동**: WebSocket과 Redis Pub/Sub 완전 연동
- **데이터 정합성**: Redis와 MySQL 간의 데이터 동기화 개선
- **동시성 처리**: 대량 요청 처리를 위한 동시성 개선

### 기술적 고려사항
- **확장성**: Scale-out을 고려한 아키텍처 설계
- **성능**: 대용량 트래픽 처리를 위한 최적화
- **보안**: 보다 강화된 인증/인가 시스템
- **모니터링**: 애플리케이션 상태 모니터링 시스템 도입

---

## 🛠 mvp2/feed — 프런트엔드 추가 & Docker 전환 라운드 (2026-06-17)

인스타 감성 SPA를 `src/main/resources/static/`에 붙이고, 로컬 실행을 docker compose 스택으로 전환하면서 마주친 문제와 해결 기록.

### 1) Spring Security가 모든 요청을 차단

#### 문제점
`SecurityConfig`에 `SecurityFilterChain` Bean이 없어서, Spring Security 6 기본값(모든 요청 인증 + 폼 로그인)이 적용. `JwtFilter`는 `@Component`로 떠 있어도 필터 체인에 연결 안 됨 → 정적 리소스조차 호출 불가.

#### 해결방법
- `SecurityConfig`에 `SecurityFilterChain` Bean 추가
- 화이트리스트: 정적(`/`, `/css/**`, `/js/**`, `/uploads/**`), 인증(`/auth/login`, `/members/signup`, `/kakao/**`), 공개 조회(`GET /feeds/**`, `/comments/**`, `/likes/**`, `/members/{id}`)
- 나머지는 인증 필요. CSRF off, `SessionCreationPolicy.STATELESS`
- `addFilterBefore(jwtFilter, UsernamePasswordAuthenticationFilter.class)` 로 JWT 필터 등록

### 2) `.env` 없는 컨테이너에서 부팅 실패

#### 문제점
`application.yml`의 `spring.config.import: file:.env[.properties]`가 강제 import라, 컨테이너 안에는 `.env`가 없어서 `Config data resource ... does not exist`로 부팅 실패.

#### 해결방법
`optional:` prefix 추가
```yaml
spring:
  config:
    import: optional:file:.env[.properties]
```

### 3) Redis 호스트가 환경변수로 안 들어감

#### 문제점
Spring Boot 3에서 `spring.redis.*`는 deprecated, `spring.data.redis.*`만 인식. `application.yml`에 `spring.redis.host`만 있고 `data: redis:` 블록은 잘못된 위치(최상위)라 무시 → 컨테이너 안에서 `localhost:6379`로 폴백 → `Connection refused`.

#### 해결방법
docker-compose에 정확한 키로 환경변수 주입
```yaml
SPRING_DATA_REDIS_HOST: redis
SPRING_DATA_REDIS_PORT: "6379"
```

### 4) 피드 작성 시 카카오 API 401

#### 문제점
`FeedService`가 좌표→주소 변환을 위해 카카오 Local API를 호출. docker-compose에 `KAKAO_CLIENT_ID: dummy`로 박아둬서 `wrong appKey(dummy) format`.

#### 해결방법
docker-compose에서 호스트 `.env`의 실제 키를 자동 치환
```yaml
KAKAO_CLIENT_ID: "${KAKAO_CLIENT_ID:-dummy}"
```
호스트에서 `set -a; source .env; set +a; docker compose up -d` 흐름으로 키 주입.

### 5) `Data too long for column 'access_token'` — 로그인 자체가 실패

#### 문제점
`JwtToken` 엔티티의 `accessToken/refreshToken`에 `@Column(length=...)` 누락 → MySQL 기본값 `VARCHAR(255)`. 이메일이 긴 사용자는 JWT 자체가 255자를 넘어 INSERT 실패 → 로그인 응답 400.

#### 해결방법
- 엔티티에 `@Column(length = 1024)` 추가
- 기존 DB는 직접 ALTER
```sql
ALTER TABLE jwt_token
  MODIFY COLUMN access_token  VARCHAR(1024),
  MODIFY COLUMN refresh_token VARCHAR(1024);
```

### 6) JPA 매핑 깨짐 — `Member.messages` `mappedBy="member"`

#### 문제점
부팅 시점에 `Collection 'Member.messages' is 'mappedBy' a property named 'member' which does not exist in the target entity 'Message'`. `Message` 엔티티에는 `sender`, `receiver`만 있고 `member` 필드는 없음.

#### 해결방법
```java
@OneToMany(mappedBy = "sender", cascade = CascadeType.ALL)
private List<Message> messages = new ArrayList<>();
```

### 7) `FeedServiceImpl`의 부정 조건 누락 — 본인 글을 못 고치고 못 지움

#### 문제점
```java
if (memberRepository.existsById(member.getId())) {  // ← ! 누락
    throw new NotFoundException(ErrorCode.NOT_FOUND_MEMBER);
}
```
회원이 **존재할 때** "회원을 찾을 수 없다"고 던지는 정반대 로직. 결과적으로 본인이 만든 피드도 수정/삭제 호출하면 항상 404.

#### 해결방법
`updateFeed`, `deleteFeed` 양쪽에 `!` 추가
```java
if (!memberRepository.existsById(member.getId())) { ... }
```

### 8) 회원가입 폼이 GET으로 submit → `For input string: "signup"`

#### 문제점
프런트 `<form>` 태그에 `method`, `action` 미명시 → 브라우저 기본값은 **GET + 현재 URL**. 캐시된 옛 JS 또는 핸들러 등록 전 enter 키 시 기본 동작이 발사돼서 `GET /members/signup` → 백엔드의 `@GetMapping("/{id}")`가 그 path를 잡아서 `"signup"`을 `Long`으로 변환 시도, `MethodArgumentTypeMismatchException`.

#### 해결방법
이중 가드.
- 프런트
```js
el("form", { onsubmit: onSubmit, action: "javascript:void(0)", method: "post" }, [...]);
```
- 백엔드 — path variable에 숫자 정규식 제약 (다른 path는 라우트 매칭 자체가 안 됨)
```java
@GetMapping("/{id:[0-9]+}")
public ResponseEntity<...> getProfile(@PathVariable Long id) { ... }
```

### 9) 회원가입 400의 진짜 원인이 안 보임

#### 문제점
`GlobalExceptionController`의 `@ExceptionHandler(Exception.class)`가 모든 예외를 무조건 `400`으로 매핑하면서 메시지만 반환. 진짜 원인(NPE, JPA 예외 등)이 가려져 디버깅 불가.

#### 해결방법
catch-all 핸들러에 `@Slf4j` + `log.error("Unhandled exception ...", e)` 추가. validation 핸들러에도 `log.warn(...)`. 컨테이너에는 호스트 볼륨으로 로그 파일을 노출.
```yaml
# docker-compose.yml
environment:
  LOGGING_FILE_NAME: /app/logs/app.log
  LOGGING_LEVEL_COM_EXAMPLE_NEWSFEED: DEBUG
volumes:
  - ./logs:/app/logs
```

### 10) 비밀번호 룰이 너무 약함 (`@Length(min=4)`)

#### 문제점
신규 회원의 비밀번호가 4자만 넘으면 통과하는 상태. 보안상 부적절.

#### 해결방법
- DTO
```java
@Length(min = 8, message = "비밀번호는 최소 8자입니다.")
@Pattern(
    regexp = "^(?=.*[A-Za-z])(?=.*\\d)(?=.*[^A-Za-z\\d]).{8,}$",
    message = "비밀번호는 영문, 숫자, 특수문자를 모두 포함해 8자 이상이어야 합니다."
)
private String password;
```
- 프런트 폼 input에 `minlength="8"`, 동일한 `pattern`, 안내 placeholder/title 추가 → 브라우저 단에서 1차 차단.

### 11) `MemberRequestDto.role` 누락으로 가입 400 반복

#### 문제점
`role`이 `@NotNull`인데 사용자 입력이나 캐시된 옛 페이지에서 `null`로 들어오는 케이스 발생.

#### 해결방법
프런트 폼은 항상 `role: "USER"` 자동 첨부.
폼 필수 필드 전부에 `required` 속성 추가해서 빈칸은 브라우저 단에서 차단.

### 12) 단일 세션 강제로 두 사용자 동시 로그인 불가

#### 문제점
`AuthService.login`이 `tokenRepository.existsByMemberEmailNot(email)` 로 **다른 누군가가 로그인 상태면 새 로그인 전체 거부**. e2e에서 alice + bob 두 명 동시 인증이 필요한 시나리오가 막힘.

#### 해결방법(테스트 측 우회)
e2e 스크립트에서 한 명씩 직렬화 — alice 작업 → `auth/logout` → DB의 `jwt_token` 테이블 비우기 → bob 로그인 → 작업 → logout. 백엔드 정책은 그대로 둠.

### 13) `LikeService.getLikeCount` NPE — 좋아요 0개일 때

#### 문제점
좋아요가 한 번도 없는 피드에 `GET /likes/{feedId}` 호출 시 `Cannot invoke "java.lang.Long.intValue()" because "count" is null`. JPQL의 `SUM(...)`이 NULL을 돌려주는데 그걸 `intValue()`로 깜.

#### 해결방법(임시)
e2e에서 호출 순서를 "좋아요 누른 다음"으로 옮겨 회피.
**근본 fix 권장**: 서비스에서 `count == null ? 0 : count.intValue()` 또는 JPQL에 `COALESCE(SUM(...), 0L)`.

### 14) 기존 소스 컴파일 자체가 깨져 있음

#### 문제점
브랜치 `mvp2/feed` 인계 시점에 다음 5건이 컴파일 실패 상태였음.

| 위치 | 증상 | 원인 |
|---|---|---|
| `Message.java` | `cannot find symbol BaseTimeEntity` | import 경로 오타 `constants.BaseTimeEntity` (실제는 `constants.entity.BaseTimeEntity`) |
| `LikeController.java` | `package com.example.newsfeed.auth.util does not exist` | 실제 패키지는 `com.example.newsfeed.util` |
| `JwtMemberDto.java` | `package com.example.newsfeed.auth.type does not exist` | `LoginType` 실제 위치는 `member.type.LoginType` |
| `RedisConfig.java` | `cannot find symbol ChatMessageSubscriber` | 참조 클래스 미존재 |
| `AuthenticatedMemberUtil` / `UserDetailsImpl` | `getMember()`, `getAuthenticatedMemberId()` 메서드 미존재 | 호출처(LikeController 등)는 있는데 정의가 없음 |

#### 해결방법
- import 경로 3건 수정
- `ChatMessageSubscriber`는 비어있는 `@Component` 스텁으로 신규 생성 (RedisConfig가 컴파일되도록)
- `UserDetailsImpl.getMember()` 게터 추가
- `AuthenticatedMemberUtil.getAuthenticatedMemberId()` 추가 (`SecurityContextHolder` 기반)
- `TokenRepository`에 derived 메서드 3개 추가: `findByRefreshToken`, `deleteByMember`, `deleteByAccessToken`
- `JwtProvider.getExpiration(String token)` 추가

### 15) 무한 스크롤이 `/feeds/likecount`를 두 번 짝지어 호출

#### 문제점
페이지 최초 진입 시 `IntersectionObserver`가 한 번 firing → 첫 fetch → 응답 후에도 sentinel이 viewport에 그대로 보임 → 한 번 더 firing.

#### 해결방법
last page 도달 시 observer를 끊음.
```js
if (isLast) {
  state.done = true;
  obs.disconnect();
}
```

### 16) Docker 빌드/실행 시 잡힌 사소한 이슈들

| 증상 | 원인 | 해결 |
|---|---|---|
| `/favicon.ico` 콘솔 400 | 파일 없음 + catch-all이 모든 예외를 400으로 매핑 | 동작상 영향 X. 필요시 1×1 favicon 추가 |
| 좀비 컨테이너 충돌 `nf-app already in use` | Docker Desktop 자동 종료/재기동 시 남은 컨테이너 | `docker rm -f nf-app` 후 `docker compose up -d` |
| 호스트 MySQL/Redis와 포트 충돌 | 호스트에도 같은 서비스가 떠 있음 | compose에서 `3307:3306`, `6380:6379`로 호스트 포트 변경 |
| 비-이미지 업로드(svg) 거절 못 함 | MIME 화이트리스트만 검증 | `image/jpeg`, `image/png`, `image/gif`, `image/webp` 만 통과시킴 |

### 17) e2e 스크립트 자체 함정

| 증상 | 원인 | 해결 |
|---|---|---|
| `args[@]: unbound variable` | macOS bash 3.2 + `set -u` + 빈 배열 | `set -u` 제거 |
| `jq_get` 파이프가 무시되어 PK 항상 빈 값 | heredoc `<<'PY'`가 stdin을 잡아먹어 호출 측 `< file` redirect가 무시됨 | `python3 -c '...'` 인라인으로 전환 |
| PASS/FAIL 라인이 가끔 사라짐 | `req()`가 응답 본문을 stdout으로 cat하는데, 진행 메시지도 stdout이라 호출 측 redirect가 같이 삼킴 | 진행/결과 메시지는 `>&2`로 분리 |
| `GET /` 가 406 | `Accept: application/json` 헤더 기본값 + 정적 HTML 응답 | 기본 Accept 헤더 제거 |

### 검증

`scripts/e2e.sh` 한 번에 정적 리소스 + 모든 컨트롤러 47개 케이스 통과(PASS 47 / FAIL 0). 회원가입 중복/필드 누락, 인증 가드, 좋아요 토글, 댓글/대댓글, 친구 요청·수락·삭제, 토큰 갱신·재발급·로그아웃까지 포함.

---

## 🛠 mvp2/feed — DM·친구 검색·검증 UX 라운드 (2026-06-22)

이날은 인스타식 회원가입 폼의 인라인 검증 UX, 프로필·DM·친구 검색 화면 추가, JMeter 부하 테스트 실패 분석, SockJS 토큰 유실 문제 해결을 다뤘다.

### 1) 회원가입 폼 검증 에러를 "요청 실패"로 보여주고 있었음

#### 문제점
`GlobalExceptionController.handleMethodArgumentNotValidException`이 첫 번째 에러 메시지만 `String`으로 반환하던 구조. 프런트는 그 한 줄을 generic 에러 영역에 띄울 뿐, 어느 필드가 어떻게 잘못됐는지 보여주지 못함.

#### 해결방법
- 백엔드: 응답을 `Map<String, Object>` JSON으로 변경 → `{message, fieldErrors:{field: msg}}`
- 프런트 `api.js`: `ApiError`에 `fieldErrors` 보관
- 프런트 `auth.js`(회원가입): 각 input 옆 인라인 메시지 영역 + `input.invalid` 클래스(빨간 테두리·연분홍 배경) + submit 시 첫 invalid에 focus + 사용자가 다시 타이핑하면 자동 클리어
- CSS `.field-error`, `input.invalid` 신규

```json
// 신규 응답 형식
{
  "message": "이메일 형식이 일치하지 않습니다.",
  "fieldErrors": {
    "email": "이메일 형식이 일치하지 않습니다.",
    "password": "비밀번호는 영문, 숫자, 특수문자를 모두 포함해 8자 이상이어야 합니다.",
    "phoneNumber": "핸드폰 번호는 필수 값 입니다."
  }
}
```

### 2) JMeter 부하 테스트 — 3번 실패 후 측정

#### 문제점들
1. **XML 파싱 실패** — `ResponseAssertion`의 `<stringProp>`에 `name=` 속성 누락(JMeter 5.5는 필수). signup assertion 통째로 제거 + 시드는 셸에서 미리 처리하는 방식으로 우회.
2. **`ThroughputController` percentThroughput 형식 잘못 추측** — `<floatProp>` ↔ `<FloatProperty>` 두 번 왕복. 정답: 후자(`<FloatProperty><name/><value/><savedValue/></FloatProperty>`).
3. **ARM emulation + 1000 thread → 50초 만에 OOM `Killed`** — `justb4/jmeter:5.5` 이미지가 `linux/amd64` 전용. Apple Silicon에서 에뮬레이션으로 돌면 CPU 패널티 + 메모리 비효율.
4. **POST 100% 실패** — `seed feed`가 5ms로 응답되며 100% err. `login` 단계에서 토큰 추출 실패 추정(응답에 `data.accessToken` 없는 케이스가 섞임). 부하 시작 전 시드 사용자를 `curl`로 미리 가입시키고 토큰을 `-J` 옵션으로 주입하는 방식이 더 안정적.

#### 해결방법(차기 라운드에서 적용 예정)
- 멀티아키 이미지(`apache/jmeter`) 사용
- 시드/토큰은 호스트 셸로 미리 발급해 `-JTOKEN=...`로 주입
- 스레드 1000 → 300, 시간 60s로 축소해 손익분기 측정

### 3) `GET /members/signup → "For input string: 'signup'"`

#### 문제점
프런트 `<form>` 태그에 `method`/`action` 미명시 → 브라우저 기본값(GET + 현재 URL)으로 새는 케이스. 캐시된 옛 JS나 핸들러 등록 전 enter 키 시 발동 → `GET /members/signup` → 백엔드의 `@GetMapping("/{id}")`가 `"signup"`을 `Long`으로 변환 시도 → `MethodArgumentTypeMismatchException`.

#### 해결방법
이중 가드.
- 프런트: `el("form", { onsubmit, action: "javascript:void(0)", method: "post" }, ...)`
- 백엔드: path variable에 정규식 — `@GetMapping("/{id:[0-9]+}")`, `@GetMapping("/{feedId:[0-9]+}/member")`. 비-숫자 path는 매핑 자체에 안 잡힘.

### 4) DM(다이렉트 메시지) 절반만 구현돼 있던 상태

#### 문제점
`MessageService.sendMessage`는 있지만 `WebSocketConfig`, STOMP endpoint, handshake 인증, REST controller 모두 부재. `Map<Long, WebSocketSession>` 의존성도 등록처 없음 → 빈 Map만 주입돼서 "오프라인" 분기만 타는 상태.

#### 해결방법(풀스택 추가)
- `WebSocketConfig` — `/ws` endpoint, 브로커 `/topic` `/queue`, user prefix `/user`
- `JwtHandshakeInterceptor` — `?token=` query param 검증 → memberId를 attribute에 stash
- `JwtHandshakeHandler` — attribute의 memberId를 STOMP `Principal`로 promote (`convertAndSendToUser` 라우팅용)
- `MessageController` REST — `POST /messages`, `GET /messages/with/{peerId}`, `/unread`, `/inbox`, `PATCH /messages/{id}/read`
- `MessageService` 리팩 — `userSessions` 제거, `SimpMessagingTemplate.convertAndSendToUser(receiverId, "/queue/messages", dto)`로 푸시
- `MessageResponseDto` 신규
- `MessageRepository.findConversation(me, peer, pageable)` — 양방향 대화 페이징

프런트 — `js/dm.js`(전역 STOMP 클라이언트, 인박스, 대화 화면), `index.html` STOMP CDN, `js/main.js`에 `#/dm`, `#/dm/:peerId` 라우트 + 💬 아이콘 + 로그인 시 자동 connect / 로그아웃 시 disconnect, CSS DM 버블 스타일.

### 5) SockJS가 `?token=...`을 sub-URL에 안 실어 보냄 → 무한 재시도

#### 문제점
처음엔 `withSockJS()` + 클라이언트 `new SockJS('/ws?token=...')` 조합으로 갔다. 하지만 SockJS는 `/info` 처음 호출 이후 `xhr_streaming`, `htmlfile` 같은 fallback transport sub-URL(`/ws/{server}/{session}/xhr_streaming`)을 만들 때 **base URL의 query string을 떨굼**. → 매 transport request마다 토큰 없는 핸드셰이크 → `JwtHandshakeInterceptor` 거절 → SockJS가 재시도하면서 로그 폭발 + 클라에 "요청 실패" 토스트.

#### 해결방법
SockJS 폴백을 빼고 **raw WebSocket + STOMP**로 직접 연결. `?token=...`이 URL upgrade 단계까지 그대로 유지됨.
- 백엔드 `WebSocketConfig`에서 `.withSockJS()` 제거
- `index.html`에서 sockjs-client CDN 제거 (StompJs만 유지)
- `dm.js`에서 `webSocketFactory: () => new SockJS(...)` → `brokerURL: 'ws://host/ws?token=...'`로 전환

### 6) DM 인박스가 "요청 실패"로 사용자를 놀라게 함

#### 문제점
`/friends`가 200 + `content:[]`로 정상 응답해도 친구 0명일 때, 그리고 진짜 4xx/5xx로 실패할 때 똑같이 `친구 목록 로딩 실패: <err.message>`로 보여줬다. 양쪽 다 의도된 UX가 아님.

#### 해결방법
실패와 빈 결과를 같은 빈 상태(💬 + "아직 친구가 없어요" + 친구 찾기 안내)로 통합. 진짜 실패한 경우만 작은 회색 글씨로 한 줄 부가:
```
(목록을 잠시 가져오지 못했어요 — 새로고침 후 다시 시도해주세요)
```
디버깅은 콘솔의 `[dm] /friends failed:` 로그.

### 7) 친구를 추가할 방법이 없음(검색 API 부재)

#### 문제점
백엔드에 멤버 검색 endpoint가 없었다. 친구 요청 보내려면 receiverId를 알아야 하는데, 사용자가 ID를 외울 리 없음.

#### 해결방법
- `MemberRepository.searchByQuery(q, pageable)` — `LOWER(name)` / `LOWER(email)` `LIKE %q%`, `deletedAt IS NULL`
- `MemberSearchDto` — `{id, name, email, image}`
- `MemberService.searchMembers` + `MemberServiceImpl` 구현
- `MemberController.GET /members/search?q=&page=&size=` (CommonResponse 래핑)

프런트 — `js/friends.js`(4탭: 친구 / 받은 요청 / 보낸 요청 / 친구 찾기, 디바운스 250ms 검색 + "친구 요청" 버튼), `main.js`에 `#/friends` 라우트 + 👥 아이콘, CSS 탭/카드 스타일.

### 8) 프로필 화면 (인스타식 3열 그리드)

#### 추가 사항
- `js/profile.js` 신규 — 헤더(아바타 + 닉네임 + 게시물 수) + 3×N 그리드
- `main.js`에 `#/profile/:id` 라우트 + 👤 아이콘
- CSS `.profile-head`, `.profile-grid`, `.grid-cell` 스타일

알려진 한계: `FeedResponseDto`가 `feedId`를 내려주지 않아 그리드 셀 클릭 시 디테일로 이동을 못함(현재는 toast로 제목만 표시). `HANDOFF.md`의 HIGH-5 항목으로 후속 처리 예정.

### 9) 좋아요 "요청 실패" 토스트 (사용자 보고)

#### 진단
직접 curl로 `POST /likes/like/13` 호출 시 백엔드는 **200** 응답. 즉 서버 로직 자체는 정상.

#### 가능한 원인 두 가지
1. **비로그인/토큰 만료** 순간 클릭 — `api.js`가 401 받고 `/auth/refresh` 실패하면 `ApiError`의 fallback message `"요청 실패"`로 떨어짐. `feed.js`의 catch에서 그대로 토스트.
2. **`localStorage`의 `liked:feedId` 상태가 서버와 어긋남** — 새 브라우저(localStorage 비어있음)에서 이미 좋아요한 피드에 클릭하면 `wasOn=false`로 판단 → `/likes/like`로 호출 (중복 like). 또 반대 케이스에선 like row가 없는 피드에 `dislike` 호출되어 `LikeServiceImpl#disLike`가 `NOT_FOUND_LIKE` 404 던짐.

#### 해결방법(미해결, 차기 라운드)
- **백엔드에 "내 좋아요 여부" API 추가** (`GET /likes/{feedId}/my`) — 클라이언트가 서버 권위로 상태 판단
- 또는 `LikeServiceImpl`을 멱등화(같은 사용자가 두 번 like 호출하면 idempotent), `disLike`도 like row 없으면 200 noop 처리
- `HANDOFF.md`의 HIGH-1(`getLikeCount` NPE)와 함께 묶어서 처리 권장

### 10) 정리한 운영용 흐름

| 액션 | 명령 |
|---|---|
| 환경 살리기 | `docker compose up -d` |
| 새 코드 반영 | `docker compose up -d --build app` |
| 토큰 비우기(단일 세션 우회) | `docker compose exec -T mysql mysql -uroot -p1234 newsfeed -e "DELETE FROM jwt_token;"` |
| 부하 테스트 | `docker run --rm -v $(pwd)/loadtest/jmx:/jmx -v $(pwd)/loadtest/results:/results --network newsfeed_default justb4/jmeter:5.5 -n -t /jmx/load.jmx -l /results/result.jtl -e -o /results/report` |
| 회귀 검증 | `./scripts/e2e.sh` (52/52 PASS) |

### 검증

이날 작업 후 `scripts/e2e.sh`는 **52 / 52 PASS**. `/messages/*`, `/members/search`, `/ws/info(→ raw WS endpoint)` 추가 smoke test 통과.

---

## 2026-08-25 — 2계정 E2E 브라우저 검증에서 발견한 버그 + 수정

Alice(memberId 130) / Bob(131) 두 계정으로 게시/좋아요/댓글/대댓글/친구/DM 전 기능을 실제 브라우저로 돌려 확인. 아래는 발견 → 근본원인 → 수정.

### B1) 게시글 작성이 사실상 막힘 (위치 "(선택)"인데 실제 필수)
- **증상**: 컴포저에서 사진+제목 넣고 공유 → "요청 실패". 이미지 업로드(`/files/image`)는 200인데 `POST /feeds`가 400.
- **원인**: 프런트는 항상 `latitude/longitude: null` 전송 → 백엔드가 주소로 카카오 지오코딩을 **강제**. 빈 주소 → "주소 또는 좌표 정보가 충분하지 않습니다", 영문 주소 → "좌표를 찾을 수 없습니다"(카카오 400). 실제 한국 주소만 성공. (`FeedServiceImpl.registerFeed`)
- **수정**: 위치를 진짜 선택사항으로. 주소/좌표 둘 다 없으면 위치 없이 게시 허용. 주소만 있으면 좌표 보완, 좌표만 있으면 주소 보완. 지오코딩으로 얻은 좌표를 실제로 저장(기존엔 계산만 하고 DTO의 null을 저장하던 버그도 동반 수정). `FeedRequestDto.toDto`가 보정된 address/lat/lng를 받도록 변경.

### B2) 프로필이 항상 "0 게시물"
- **증상**: Alice가 글을 올려도 `#/profile/130`에서 "아직 게시물이 없어요". `/feeds/members/130` → 200이지만 빈 배열(작성 글 author.id=130 확인됨).
- **원인**: `FeedController.getFeedsByMemberId(Long memberId)`에 **`@PathVariable` 누락** → `{memberId}` 미바인딩(null) → 쿼리 결과 없음.
- **수정**: `@PathVariable Long memberId`.

### B3) DM이 엉뚱한 회원에게 전송됨 → 실제 미전달 (Critical)
- **증상**: Alice가 Bob과 친구를 맺고 DM 전송했으나 Bob 수신 0건. 대화창이 `#/dm/18`로 열리고 상대 이름이 "Alice"/"user18"로 표시.
- **원인**: `FriendServiceImpl.findFriendList`가 `new FriendListDto(friend.getId(), friend.getSender().getName())` — **Friend 테이블 row PK(18)** 를 회원 id로, **항상 sender(Alice)** 를 이름으로 반환. 프런트는 이 id를 DM 수신자 id로 사용 → 유령 member 18로 전송.
- **수정**: 로그인 회원 기준 **상대방(counterpart)** 의 memberId + name 반환.

### B4) 받은/보낸 친구요청 목록이 본인 정보를 잘못 표시
- **원인**: `findReceivedFriendRequests`가 요청자(sender)가 아닌 `friend.getReceiver()`(=나) 반환 + DTO 인자(email,name) 순서 뒤바뀜. `findSenderInfo`(보낸 요청)도 receiver가 아닌 sender 반환 + 프런트가 읽는 필드명(receiver*)과 DTO 필드명(sender*) 불일치로 "?" 표기.
- **수정**: 받은 요청은 sender의 email/name을 올바른 슬롯에, 보낸 요청은 receiver의 email/name을 프런트가 읽는 필드명(receiverEmail/receiverName)으로 반환.

### B5) [UI] 상세 페이지 사진 잘림 + 댓글 영역 과대
- **원인**: `.detail`이 `--max: 470px` 좁은 컨테이너에 `grid-template-columns: 1fr 360px`(댓글 고정 360px)라 사진이 눌림.
- **수정**: 상세/DM 화면 컨테이너를 넓히고(`:has`) `.detail`을 **사진:댓글 = 6:4**(`6fr 4fr`)로.

### B6) [UI] DM 첫 메시지가 헤더에 가려짐
- **원인**: `.dm-conv-head { position: sticky; top: 54px; z-index: 5 }` 가 스크롤 컨테이너 상단 메시지를 덮음(버블은 DOM엔 정상 렌더).
- **수정**: sticky 제거(플렉스 컬럼 상단 고정이라 불필요).

### B7) DM 시간 "9시간 전" 오차
- **원인**: 앱 컨테이너 TZ=UTC라 `LocalDateTime.now()`가 UTC. 프런트는 타임존 없는 값을 로컬(KST)로 파싱 → +9h.
- **수정**: docker-compose app 서비스에 `TZ: Asia/Seoul`.

### B8~B10) 경미 항목도 후속 처리 완료
- **B8 댓글 작성자 실명**: `CommentResponseDto`에 `authorId/authorName` 추가(`comment.getMember()`), `detail.js commentRow`가 실명 표시. (기존 "user" 하드코딩 제거)
- **B9 대댓글 UI**: `detail.js`에 최상위 댓글마다 "답글 달기" 입력 추가 → `POST /comments {parentId}`. 백엔드는 이미 1단계 대댓글 지원.
- **B10 DM/프로필 상대 실명**: `MemberResponseDto`에 `name/email/image` 추가 → `/members/{id}`가 실명 반환. DM 상대 "user131"→"Bob", 프로필 이름도 정상.
