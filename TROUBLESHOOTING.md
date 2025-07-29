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