# 🎈 newsFeed 🎈
## 🛠️ Tools :  <img src="https://img.shields.io/badge/mysql-4479A1?style=for-the-badge&logo=mysql&logoColor=white"> <img src="https://img.shields.io/badge/spring-6DB33F?style=for-the-badge&logo=github&logoColor=Green"> <img alt="Java" src ="https://img.shields.io/badge/Java-007396.svg?&style=for-the-badge&logo=Java&logoColor=white"/>  <img alt="Java" src ="https://img.shields.io/badge/intellijidea-000000.svg?&style=for-the-badge&logo=intellijidea&logoColor=white"/> <img src="https://img.shields.io/badge/Socket.io-FF0000?style=flat-square&logo=socket.io&logoColor=white"> <img src="https://img.shields.io/badge/Redis-FF0000?style=flat-square&logo=redis&logoColor=white">
## 🥅 Period : 2024/12/16 ~ 2024/12/31
## 👨‍💻 ERD :
## 👨‍💻 API : 
## 👨‍💻 About Project :
작은 일상을 공유할 수 있는 소셜 미디어. 회원 가입 후 개시물 생성, 팔로우 기능, 댓글, 메신저 기능이 구성되어 있으며, 개시물 생성시, 사진과 함께 위치, 그리고 글 작성이 가능한 프로젝트.
## 1. MVP1
###  Member
 - create
     1. 회원 생성시 PasswordEncoder를 활용해, 비밀번호 암호화
     2. 회원에 대한 각종 정보 생성후 JWT 토큰 발급
   
- update
     1.  회원 정보 수정, 수정시 비밀번호 확인 필요
    
- findOrCreateMember
     1. 카카오로 로그인 한 회원 바로 회원 가입 진행
     우선 카카오 회원 조회를 한 후 생성, => 회원 조회 or 생성 해서 가입한 적이 없다면 바로 로그인시 가입 처리

- getMemberById
     1. 회원 아이디 조회

- getMemberByFeedId
     1. 피드 아이디 기반 회원 조회
     2. 이 기능이 Feed Service에 들어가는게 맞는지 아님 MemberService에 들어가는게 맞는지 고민이 되었다. MVP 2 작성시 위치 변겅 가능

- deleteMemberById
     1. 회원 삭제시 softDelete & 삭제시 비밀번호 검증 필요

- changePassword
     1. 비밀번호 변경
     2. 오래된 비밀번호와, 신규 비밀번호를 입력 단, 기존 비밀번호와 신규 비밀번호가 동일하면 안됨

### Feed
- createFeed
     1. 피드 생성, 단 생성 전에 해당 회원에 대한 pk값 검증
     2. 피드 작성시 해당 위치를 입력하면 현 주소 나옴.

- getFeed
     1. 피드에 대한 정보 가져옴

- getFeedsByMemberId
     1. 회원 아이디 기반 피드 조회
     2. MemberService에 getMemberByFeedId의 기능과 바꿔야 하는건 아니지에 대한 고민 MVP2때 변경 가능성 있음

- getAllFeedsOrderByLikeCount
     1. 피드 좋아요가 많은 순으로 정렬. 좋아요가 많이 찍힐 수록 인기 개시물이기 때문에 내림차순으로 정렬 할 수 있도록 함

- updateFeed
     1. 피드 정보 수정.
     2. 위치, 주소, 이미지, content, title등 정보 수정 가능

- deleteFeed
     1. 해당 피드 삭제

### Friend 
- addFriend
     1. 친구 pk 값으로 조회 후 친구 신청 기능.
     2. 단 Session이 보내는 사람의 pk 값과 동일하지 않다면, 보낼 수 없다. 사용자가 로그인을 하지 않았고, 친구 요청을 보낸 적이 없는데 임의로 누군가, 해당 사용자 아이디로 다른 사용자에게 보내면 안된다고 생각이 들어 Session 값을 검증 하도록 했다. 
     3. 또한 이미 친구 관계라면 다시 한번 친구 요청을 보낼 수 없다.

- acceptFriendRequest
     1. 친구 요청 수락
     2. Session이 해당 로그인 사람의 아이디와 동일하지 않다면 친구 수락 할 수 없음
     3. ACCEPTED로 변경 되면서 친구 요청 수락

- findSenderInfo
     1. 보낸 사람의 정보를 알아야 할 필요성이 있다고 생각해 이 기능을 넣었다. 모르는 사람일 경우 누군지 확인할 필요성이 있다고 생각이 들었다.
        한번쯤은 사용자 관점에서 보고 생각해 위 기능을 만들었다.

- findReceivedFriendRequests
     1. 친구 요청이 온 request를 수락 전에 확인 할 수 있으며, 수락을 하면 해당 Request는 사라진다.

- findFriendList
     1. 친구 전체 목록 조회

- deleteFriend
     1.  친구 삭제 기능
 
### Like
- like
     1. 좋아요 기능
        피드에 좋아요를 누를 수 있는 기능을 만들었다.
- disLike 
     1. 좋아요 취소 기능
        좋아요를 누르지 않은 개시물에는 좋아요 취소를 할 수 없고, 좋아요가 눌린 첫번 째 값을 제거

- getLikeCount
     1. 해당 피드의 좋아요 수 확인 가능

### Auth 
- 일반 User Login
  회원 가입 후, 해당 email & password 입력하면 Login 가능.
  
- Kakao 로그인
  카카오 외부 API를 받아 로그인 가능. 카카오에서 AccessToken을 받은 후 클라이언트가 전달한 카카오 AccessToken을
  내 Application 사용을 위한 token으로 재발급해서 로그인 가능.

- JWT + Session의 혼합
1. 세션을 선택한 이유는 우선 웹 서버에 저장을 하기 때문에 매번 Application을 실행 할때마다 새로운 Session을 형성해, 보안적인 면에서 쿠키보다 좋다고 느꼈다. 어떤걸 선택하던, 개발 애플리케이션의 방향성에 따라 다르겠지만, 보안에 좀더 초점을 두고 작업을 진해하고 싶었다.

2. Interceptor를 선택 한 이유
Filter와 인터셉터는 MVC에서 잡는가 아니면 DispathcerServlet에서 잡는가의 위치 차이라고 생각한다. 단 두개의 필요성은 모든 인증인가를 Controller에서 작업을 진행하면 중복 코드 증가하고, 수정 필요시 작업이 번거로워지기 때문에 두 기능을 사용 한다.
그리고 이번에 나는 Intercpetor를 사용했다.

3. Interceptor + Session + JWT를 왜 사용 했는가?
우선 MVP1에서는 Session과 인터셉터를 이용해 인증 인가 부분을 처리를 했다. 그리고 JWT를 추가해 인터셉터에서 JWT 유효성 검증 및 사용자를 식별 하는 용도로 사용했다. MVP2에서 Spring Security를 도입해 인터셉터와 세션을 대체하고 JWT를 적극 더 활용하기 위해 JWT를 MVP1에서 넣은 것도 없지 않아 있다. 

### Message 
- 메시지를 보내면 MySQL에 보내고, 보낸 Message를 받는 User가 읽음 처리로 하면 MySQL에서 해당 메시지가 읽음 처리로 변경
  1) 처음에는 관계형 DB로 Chatting Program을 작서했다. 하지만 관계형 DB만 사용했을 경우 대량의 동시성 요청 처리가 올 경우에는 동시성 처리 하는데 한계가 있어, 병목 현성이 발생 가능
  2) 쓰기 작업 부담에 대한 부분도 있었다. 매번 메시지 전송 시 데이터를 영구적으로 저장해야 하고, Transaction 관리와 디스크 I/O 작업이 매번 필요 했다.
  3) 확장성의 부족 또한 고민을 안 할 수 없었다. 관계형 DB는 Scale Up에 더 적합하다고 알고 있고, Scale Out은 가능할 수는 있지만 어렵다고 알고 있다. 채팅 시스템은 사용자와 메시지 수가 증가 하면서 수평 확장이 더 적헙하다. 관계형 DB에 데이터 분할을 적용하면 복잡성이 증가할 수도 있다.
  4) 메시지를 저장하고 읽기 쓰기를 위해서는 Transaction이 필요하다. 락의 경우 다수의 클라이언트가 동일한 데이터를 읽거나 쓰려고 하면 테이블에 락이나 행락으로 인한 대기 시간이 증가할 수 있다.

- 이런 Side Effect가 있어 관계형 DB 대신, Redis 사용하는 방법을 채택 했다.
  1) Redis의 경우 Scale out에 용이하고, 대량의 데이터를 빠르게 처리 가능.
  2) Redis는 어떤 방형으로 사용하느냐에 따라 달라지겠지만, 나의 경우에는 읽기와 쓰기가 매우 빠른 점을 이용하기로 했다.
  3) Redis는 pub/sub 기능을 제공하기 때문에 채팅 프로그램에 더욱 적합 하다고 판단.
  4) Redis라고 모든 것이 좋다고 할 수 없다. 메모리 기반 데이터 저장소이기 때문에 서버 다운되면 모든 데이터 소멸
  5) 그래서 Redis에 저장된 메시지를 관계형 DB에 저장하는 방법을 생각해봤다.

- Socket과 SSE의 고민
  1) 처음 생각 한 방법은 SSE 방법이다.
  2) SSE(Server-Sent Events)는 서버에서 클라이언트로 단방향 메시지를 보내는 기술이다.
  3) 구현은 간편하고, 서버에서 클라이언트로 메시지를 보내는 방식이기 때문에 서버의 부하를 줄일 수 있다.
  4) 하지만 SSE는 HTTP/1.1 기반으로 동작하기 때문에 HTTP/2와 같은 최신 프로토콜을 사용할 수 없고,
  5) 서버에 부하가 많아지면 성능이 저하될 수 있다.
그래서 SSE 방법을 사용하지 않고 WebSocket을 사용하기로 결정 

### 👨‍💻 확장 & 생각해 봐야 할 요소들 
MVP1에서는 성능 보다는 기본적인 필요한 기능들을 채워넣는 것이 1차 목표였고, Redis와 Socket 연동에는 실패 했지만 연동까지가 목표였다. 
Security를 넣는 대신 Interceptor와 Session을 활용해 Security가 왜 필요한지, 그리고 Security룰 활용하지 않는 환경에서 라면 Session과 인터셉터 처리를 할 줄 알아야 한다고 생각이 들었다.

MVP2에서는 TestCode, Security, Socket과 Redis 연동, Redis와 MySQL의 DB 적합성, 동시성 문제에 대해 고민을 해보겠다. 
 
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
3. Logout
로그아웃 처리시 제대로된 로그아웃이 진행이 안됨
그래서 해당 회원에 조회되는 토큰이 있는지 조회 후 해당 토큰을 삭제 
```
    @Transactional
    public void logout(Long memberId) {
        JwtToken jwtToken = tokenRepository.findByMemberId(memberId)
                .orElseThrow(() -> new IllegalArgumentException("Token does not exist"));
        tokenRepository.delete(jwtToken);
    }
```

## MVP 2 
1. Spring Security 사용 
- securityFilterChain
  - 인증이 필요 하지 않은 부분을 제외 한 나머지 로그인 을 한 후 인증이 될 수 있도록 설계
- roleHierarchy
  - 권한에 따른 계층 설정 
 
2. SpringSecurity를 사용하면서, 각 기능에 있는 Controller에 있던 Session을 걷어내고, AuthenticatedMemberUtil이라는 클래스를 만들어 controller에 적용, 단 아직 해결 하지 못한 부분은
   controller에 ```Long memberId = AuthenticatedMemberUtil.getAuthenticatedMemberId();``` 중복적으로 남아 해결 방법에 대해 고민 해야 할 것 같다.

고민 해본 방향 
```
2. SpringSecurity를 사용하면서, 각 기능에 있는 Controller에 있던 Session을 걷어내고, AuthenticatedMemberUtil이라는 클래스를 만들어 controller에 적용, 단 아직 해결 하지 못한 부분은
   controller에 ```Long memberId = AuthenticatedMemberUtil.getAuthenticatedMemberId();``` 중복적으로 남아 해결 방법에 대해 고민 해야 할 것 같다.

=> @ControllerAdvice을 활용해보자. 모든 Controller에서 memberId를 공통적으로 사용할 수 있도록 모델에 바인딩 하면 코드 중복을 줄일 수 있지 않을까?? 
```

3. 왜 Security를 선택 했는가?
   filter와 인터셉터를 직접 처리해주는 것도 좋다. 하지만, 각 기능별 Filter 혹은 인터셉터를 지속적으로 클래스에 만들다 보면, 클래스가 많아져, 보다 구조가 복잡해 질 것 같다 라는 생각이 들었고,
   Security를 사용함으로써 인증 인가 처리 부분에 대해 좀더 보다 쉽게 구현 할 수 있고, 조작 할 수 있어서 선택을 했다.
   하지만 직접 필터와 인터셉터를 직접 구현하는 것보다 좋은가? 라고 하면 내가 시큐리티에 대해 미숙해서 그런지는 모르겠지만, 둘의 보안성 측면에서의 차이는 모르겠다. 물론 CORS, CSRF 등에 대한 부분은 좋을 수 있다. 
   단순 작업을 좀더 간편화 해주는 것일 뿐이지, 크게 성능이 올라간다거나 그런 면에서의 장점으 못느꼈던 것 같다. Security를 사용하면서 느낀점은 유지 보수 성 면에서 좋다이다. 


### 🥵 Trouble Shooting 
1. Session 값 걷어내면서의 문제점 발생
Security를 정확히 잘 활용하지 못한 상태에서 Session 값을 걷어내다 보니, Security의 값은 SecurityContextHolder에서 검증을 진행해야 하는데 그게 아닌 RequestAttribute로 진행하려고 하니, 적용이 안되었다. 

그래서 처음 생각한 방향은 @AuthenticationPrincipal을 각 Controller 마다 적용 하는 방법이었다.
그러나, 생각보다 계속 중복 코드가 많았고 이를 Util로 빼야 겠다고 생각이 들어 Util class로 변경했다. 
하지만 일부 중복은 피하지 못한 것 같다. 

2. Friend 요청을 보낼 때 해당 로그인한 User가 아닌데 친구 요청 및 수락을 보낼 수 있던 상황
Session 값을 걷어내고 잘못된 방법으로 해당 User인지 비교하는 validate를 처리 해주지 않아 문제가 발생
FriendService에 적용하는게 맞다고 생각했다. 향후 validate가 증가할 경우, controller에 처리하게 되면 controller의 복잡성이 증가하고,
역할에 대해 맞지 않는다고 생각했다. 
```
        if (!authenticatedMemberId.equals(friendRequestDto.getReceiverId())) {
            throw new NoAuthorizedException(ErrorCode.NO_AUTHOR);
        }
```
validate 처리 해준 후 해결 할 수 있었다. 

3. Security를 사용함으로써 userId 값 비교가 아닌 username의 비교
UserDetailService에서 username 값을 가져와야 했다. 하지만 userId로 값을 계속 가져오고 있던 탓에 
인증인가 부분에서 계속 막혔었고, 문제를 발견 후, userId 값을 가져오던 부분을 username으로 가져 올 수 있도록 변경
그리고 나중에 찾아보니 custom하게 userId로도 사용 할수 있다는 것을 알았다.
하지만 고민인 부분은 session에서야 pk 값으로 조회해서 가져오는게 빠르고 보안적 측면에서도 좋았지만, token에 각종 정보가 담겨있을때는 token에 있는 정보 값중 하나를 가져와서 비교할때 pk값을 가져와야 할때와 email을 가져와야 할때의 장단점의 구분이 아직 명확이 되지는 않는다.
email의 경우 : 사용 친화적이고, pk 값은 노출하지 않는 장점이 있지만 변경 가능성이 매우 높아 식별자로 사용하기는 애매하지 않을까 라는 생각이 든다.
pk : 단순 숫자이기 때문에 정보가 노출된다 하더라도, 문제가 되는 부분은 크게 없다고 생각이 든다. 

내가 선택한 방법은 
1. AuthService에선 email 값으로 인증 과정을 진행하고, 사용자 친화적 설계 방향을 선택.
2. pk를 내부적으로 활용하기 위해 JwtMemberDto에 member.getId pk 값을 포함시켜 내부적으로는 pk를 사용하는 방향을 선택
   조회라던지 연산간에서 유일성과 성능 면에서 유리하다고 판단 


