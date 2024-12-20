# 🎈 newsFeed 🎈
## 🛠️ Tools : 
## 🥅 Period : 2024/12/16 ~ 2024/12/31
## 👨‍💻 About Project :
## 👨‍💻 ERD
## 🧨 Trouble Shooting
### Member
Refactoring : sender와 receiver의 mapping id 값이 잘못 설정 되어 있었음

### Friend
1) Session 사용자 검증 로직 부재
문제가 있었덩 코드      
```
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
개선된 코드 
```
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
2) 친구 수락시 Friend DB의 PK 값으로 수락이 되는 문제
내가 의도 했던 바는 요청 PK가 아닌 회원 pk (고유 아이디)로 요청을 보냈었으면 했음

문제가 있었던 코드 
```
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
해결 한 코드 
```
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
### Comment
1) NPE

문제가 있었던 코드
```
public static CommentResponseDto toDto(Comment comment) {
    return CommentResponseDto.builder()
            .commentId(comment.getId())
            .content(comment.getContent())
            .feedId(comment.getFeed().getId())
            .childComments(comment.getChildren().stream() // NullPointerException 발생
                          .map(CommentResponseDto::toDto)
                          .toList())
            .build();
}
```
해결한 코드 
```
public static CommentResponseDto toDto(Comment comment) {
    return CommentResponseDto.builder()
            .commentId(comment.getId())
            .content(comment.getContent())
            .feedId(comment.getFeed().getId())
            .childComments(comment.getChildren() != null ? 
                           comment.getChildren().stream()
                                  .map(CommentResponseDto::toDto)
                                  .toList() : new ArrayList<>()) // Null 방지
            .build();
}
```

2) 회원 정보 누락

문제된 코드 
```
Comment childComment = Comment.builder()
        .content(requestDto.getContent())
        .feed(parentComment.getFeed())
        .build();
```

해결한 코드 
```
Comment childComment = Comment.builder()
        .content(requestDto.getContent())
        .feed(parentComment.getFeed())
        .member(member) // 작성자 정보 설정
        .build();
```

3) JSON 응답시 대댓글이 부모 댓글과 분리

문제 코드
```
public List<CommentResponseDto> getCommentsByFeedId(Long feedId) {
    List<Comment> comments = commentRepository.findByFeedId(feedId);
    return comments.stream()
            .map(CommentResponseDto::toDto) // 모든 댓글을 1차원 리스트로 변환
            .toList();
}
```
해결 코드 
```
public List<CommentResponseDto> getCommentsByFeedId(Long feedId) {
    List<Comment> comments = commentRepository.findByFeedId(feedId);

    return comments.stream()
            .filter(comment -> comment.getParent() == null) // 부모가 없는 댓글만 필터링
            .map(CommentResponseDto::toDto)
            .toList();
}
```

4) Lazy Loading 문제로 대댓글이 로드되지 않음
문제 코드
```
@Query("SELECT c FROM Comment c WHERE c.feed.id = :feedId")
List<Comment> findByFeedId(@Param("feedId") Long feedId);
```
해결 코드 
```
Fetch join 으로 해결 
@Query("SELECT c FROM Comment c LEFT JOIN FETCH c.children WHERE c.feed.id = :feedId")
List<Comment> findByFeedId(@Param("feedId") Long feedId);
```
### 카카오톡
1) 인증 인가 받기 이후, 리다아랙트 URI에서 Access Token 발급으로 로그인 또는 가입 처리 후, 필요한 서비스 페이지로 이동이 되야 했지만, 리다이렉트 URI에 머물러 사용자가 새로고침 할 수 있는 경우, 이미 사용된 인가 코드를 이용한 토큰 발급 요청을 다시 시도하게 되면 Error 발생.
즉 이미 받았는데 redirect로 인하여, 유효했던 코드가 이미 사용처리 된 것으로 인식디 되는 문제 있었다.
=> Controller에서 토큰을 받을때 redirect를 내가 직접 지정할 필요가 없었는데 구현을 해놓았기 때문에 두번 호출 문제 발생

2) Access Token으로 내가 작성한 Feeds나 다른 기타 기능들을 사용하려고 했는데 카카오에서 발송한 Access Token은 카카오톡 유저 정보 즉, 카카오로만 사용 가능한 토큰, 그래서 그 토큰을 내 application에 맞는 token으로 치환 필요성이 있었음
```
 public ResponseEntity<CommonResponse<Map<String, String>>> kakaoLogin(@RequestHeader("Authorization") String kakaoAccessToken) {
        // 카카오 Access Token으로 사용자 정보 조회 후 JWT 생성
        KakaoUserInfoResponseDto userInfo = kakaoService.getUserInfo(kakaoAccessToken);
        Map<String, String> jwtTokens = kakaoService.createJwtFromKakaoUserInfo(userInfo);

        return ResponseEntity.ok(new CommonResponse<>("로그인 성공", jwtTokens));
    }
```
파라미터로 Code를 받을 필요 없었다. 

3) Redirect_URI setting 문제점 => redirect_uri setting이 잘 되어 있다고 생각했지만, 그게 아니었고, HTTPS인지 HTTP인지 그리고 //가 두개가 들어가는지 하나가 들어가는지 확인할 필요가 있음

### Interceptor 
1) 회원을 생성하는데 인터셉터에서 회원 생성로직을 막아 생성이 되지 않는 문제 있었다.
원인을 보면 HIGHEST_PRECEDENCE level이 애매했고, excludePatterns을 사용해 interceptor가 적용되지 않는 부분을 따로 서렂ㅇ

2) 인코딩 문제
인코등 문제가 발생해 Message가 깨져 나왔다.
```
    @Bean
    public CharacterEncodingFilter characterEncodingFilter() {
        CharacterEncodingFilter filter = new CharacterEncodingFilter();
        filter.setEncoding("UTF-8");
        filter.setForceEncoding(true);
        return filter;
    }
```

### JWT
1) 인텔리제이를 다시 시작할때 update로 설정해 놓았지만, 계속 secret Key가 reset되는 상황
문제 코드
```
public JwtProvider() {
    this.secretKey = Keys.secretKeyFor(SignatureAlgorithm.HS256);
}
```
변경 코드 
```
public JwtProvider(@Value("${jwt.secret}") String secret) {
    this.secretKey = new SecretKeySpec(secret.getBytes(StandardCharsets.UTF_8), "HmacSHA256");
}
```
yml에 JWT 고정 값 설정 해주었음 

### Feed
1) 
```
Type definition error: [simple type, class org.hibernate.proxy.pojo.bytebuddy.ByteBuddyInterceptor]
```

2)
Hibernate가 Lazy 로딩 된 엔티티 프록시 객체를 JSON 직렬화 하려고 할때 발생 => Entity로 받는것이 아닌 DTO로 받음 
```
org.hibernate.loader.MultipleBagFetchException: cannot simultaneously fetch multiple bags:
```
OneToMany ~Many를 사용했을때 Entity 두개를 fetch join에 가져오는 것이 문제가 됨 => JsonIgnore로 해결 

3) 피드 좋아요 내림차순 정렬시 좋아개 개수가 반영되지 않는 문제
=> 전체 개수를 가져오지 않아서 문제가 발생되었다.

### Session Error 
```
PreparedStatementCallback; bad SQL grammar [DELETE FROM SPRING_SESSION WHERE SESSION_ID = ? AND MAX_INACTIVE_INTERVAL >= 0 ]
```
스프링 Session table이 셍성되지 않아서 뜨는 문제.
yml에 추가 
```
  session:
    store-type: ${SPRING_SESSION_STORE_TYPE}
    jdbc:
      initialize-schema: always
```

### 로그인 문제 
1. JWT 토큰 값이 필요할 것이라고 생각, 하지만 로그인은 단순 아이디와 패스워드 비교하기 위함이지 토큰 값까지 넣을 필요 없었음
그래서 WebConfig에서 whiteList url Pattern에 넣어줬다.

2. 로그아웃이 진행되지 않았는데 다른 계정으로 로그인 시도시 로그인이 되는 문제
AuthService에 login 메서드에 추가
```
   Optional<JwtToken> findByMemberId(Long memberId);

    boolean existsByMemberEmailNot(String email); 값을 추가 해서 로그인이 되어 있는 이메일과, pk 값 비교 
```
