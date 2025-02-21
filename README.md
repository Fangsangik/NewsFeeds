# 🎈 NewsFeed

## 📌 프로젝트 개요

**NewsFeed**는 사용자가 작은 일상을 공유할 수 있는 소셜 미디어 플랫폼입니다. 회원 가입 후 게시물을 생성하고, 팔로우, 좋아요, 댓글 및 메신저 기능을 사용할 수 있으며, 사진과 위치 정보를 함께 등록할 수 있습니다.

## 📌 ERD 
![ERD](https://github.com/user-attachments/assets/b0d8fdf6-2f33-41b0-b27f-a9510207c99c)

## 🛠️ 기술 스택

- **Backend**: Java, Spring Boot, JPA, QueryDSL, Socket.I/O
- **Database**: MySQL, Redis
- **Map Service** : KakaoMap API
- **Authentication**: JWT, Session, Kakao Login

---

## 🚀 프로젝트 기간

- **MVP 1: 2024/12/16 ~ 2024/12/31**
- **MVP 2: 2025/02/16 ~ 2024/02/31**

## 🎯 주요 기능

### 1️⃣ 회원(Member) 관리

- **회원 가입**: PasswordEncoder를 활용한 비밀번호 암호화 및 JWT 발급
- **회원 정보 수정**: 수정 시 비밀번호 확인 필수
- **카카오 로그인**: OAuth를 이용한 회원 가입 및 로그인 자동 처리
- **회원 삭제**: Soft Delete 적용, 삭제 시 비밀번호 검증 필요
- **비밀번호 변경**: 기존 비밀번호와 신규 비밀번호가 동일하지 않도록 제한

### 2️⃣ 피드(Feed) 기능

- **피드 생성**: 회원 PK 값 검증 후 피드 작성
- **위치 기반 피드 작성**: 위치 정보 등록 가능
- **피드 조회**: 회원 아이디 기반 피드 조회, 인기 게시물(좋아요 순) 정렬
- **피드 수정**: 위치, 주소, 이미지, 제목, 내용 수정 가능
- **피드 삭제**: Soft Delete 적용

### 3️⃣ 친구(Friend) 기능

- **친구 추가**: 친구 요청 발신자 검증(Session 체크), 중복 요청 방지
- **친구 요청 수락**: 요청 수락 시 ACCEPTED 상태 변경
- **보낸 요청 조회**: 요청 보낸 사람 정보 확인
- **받은 요청 조회**: 수락 전 받은 요청 확인 가능
- **친구 목록 조회**
- **친구 삭제**

### 4️⃣ 좋아요(Like) 기능

- **좋아요 추가**: 특정 피드에 좋아요 누르기
- **좋아요 취소**: 기존에 누른 좋아요만 취소 가능
- **좋아요 수 조회**: 특정 피드의 좋아요 개수 확인

### 5️⃣ 인증(Auth) 및 보안

- **일반 로그인**: Email & Password 인증 후 JWT 발급
- **카카오 로그인**: OAuth2 로그인 및 JWT 재발급
- **JWT + Session 혼합 사용**: 보안 강화를 위해 세션과 JWT를 조합하여 인증 처리
- **Interceptor 적용**: 인증 및 인가를 위한 인터셉터 활용

### 6️⃣ 메시지(Message) 기능

- **메시지 전송 및 저장**: Redis를 활용한 메시지 저장 및 조회
- **WebSocket 기반 실시간 채팅 구현**

---

## 🔧 기술적 고민 및 해결 방안

## MVP1

### 1️⃣ 인증 및 보안 강화

- **Interceptor + JWT + Session 조합**: MVP 1에서는 Interceptor와 Session을 사용하고, MVP 2에서 Spring Security를 도입
- **Refresh Token 관리**: Redis를 활용한 블랙리스트 적용
- **로그아웃 시 Token 삭제**: 회원 로그아웃 시 Redis에서 Refresh Token 제거

### 2️⃣ 카카오 로그인 리다이렉트 문제 해결

- **문제점**: 카카오 로그인 이후 리다이렉트 시 이미 사용된 Authorization Code로 인해 인증 실패 발생
- **해결 방법**: 클라이언트에서 Authorization Code를 관리하지 않고 서버에서 처리하도록 변경

### 3️⃣ 댓글 기능 개선

- **대댓글 계층 구조 유지**: 부모-자식 관계를 유지하여 JSON 응답 구조 개선
- **Lazy Loading 문제 해결**: Fetch Join을 활용하여 대댓글을 한 번의 쿼리로 가져오도록 수정

```
@Query("SELECT c FROM Comment c LEFT JOIN FETCH c.children WHERE c.feed.id = :feedId")
List<Comment> findByFeedId(@Param("feedId") Long feedId);
```

### 4️⃣ 친구 요청 검증 로직 추가

- **Session 검증 로직 추가**: 친구 요청 발신자가 본인이 맞는지 확인
- **중복 요청 방지**: 이미 친구 관계인 경우 요청 불가

```
if (!authenticatedMemberId.equals(friendRequestDto.getReceiverId())) {
    throw new NoAuthorizedException(ErrorCode.NO_AUTHOR);
}
```

---

## MVP2

## 📌 프로젝트 개요

**NewsFeed** MVP 2에서는 Spring Security를 도입하여 기존의 Interceptor 및 Session 기반 인증 방식을 개선하고, 보안성과 유지보수성을 강화하는 것을 목표로 합니다.

## 🛠️ 주요 변경 사항

### 1️⃣ Spring Security 적용

- **securityFilterChain**: 인증이 필요하지 않은 부분을 제외하고, 로그인 후 인증이 필요한 기능을 명확하게 구분
- **roleHierarchy 설정**: 역할 계층을 정의하여 권한 관리 체계 강화
- **Session 제거 및 JWT 기반 인증 적용**: 기존 Session 기반 인증을 제거하고, `AuthenticatedMemberUtil`을 활용하여 인증 정보 제공

### 2️⃣ Controller 내 중복 코드 제거 고민

- 기존 방식: `Long memberId = AuthenticatedMemberUtil.getAuthenticatedMemberId();`를 각 컨트롤러에서 반복적으로 호출
- 개선 방향:
✅ **@Authentication활용** : 인증이 요구되는 서비스에서 User를 매개변수로 직접 받아 Controller에서 Service호출 전에 검증

### 3️⃣ Spring Security 도입 이유

✅ **보안 강화**: CORS, CSRF 보호 및 인증 인가 체계 개선
✅ **유지보수 용이성**: 개별 Filter 혹은 Interceptor를 계속 추가할 경우 구조가 복잡해질 가능성이 있음 → Security를 도입하여 인증 인가 로직을 중앙에서 관리
✅ **구현 용이성**: 보안 기능을 직접 구현하는 것보다 Security 프레임워크를 활용하여 빠르고 안정적인 인증 인가 시스템 구축 가능

### 4️⃣ WebSocket + Redis 사용

- WebSocket만 사용시 Scale Out시 확장이 어려움
- Redis를 사용해 Scale Out시 이점 확보

---

## 🔧 Trouble Shooting

### 1️⃣ Session 제거 후 인증 문제 발생

**문제점:** Security를 정확히 활용하지 못한 상태에서 Session 값을 걷어낸 결과, `SecurityContextHolder`가 아닌 `RequestAttribute`를 통해 검증하려다 인증 실패 발생.

**해결 방법:**

- `@AuthenticationPrincipal`을 각 컨트롤러마다 적용하는 방법 고려
- 하지만 중복 코드가 많아 Util 클래스로 변경하여 인증 정보를 제공

### 2️⃣ 친구 요청 시 잘못된 검증 로직 문제

**문제점:** 로그인한 사용자가 아닌 경우에도 친구 요청 및 수락을 할 수 있었음.

**해결 방법:**

- `FriendService`에서 요청을 검증하는 `validate` 로직 추가

```
if (!authenticatedMemberId.equals(friendRequestDto.getReceiverId())) {
    throw new NoAuthorizedException(ErrorCode.NO_AUTHOR);
}
```

### 3️⃣ Security에서 userId가 아닌 username을 사용해야 하는 문제

**문제점:** 기존에는 `userId`를 기준으로 인증 검사를 진행했지만, Security에서는 `username`을 기본 식별자로 사용하기 때문에 인증 실패 발생.

**해결 방법:**

- `UserDetailsService`에서 `username`을 반환하도록 수정
- 필요 시 `userId`를 커스텀 필드로 추가하여 관리

### 4️⃣ PK vs Email 기반 인증 고민

**고민 내용:**

- **Email 기반 인증**: 사용자 친화적이지만 변경 가능성이 높아 식별자로 사용하기 애매함
- **PK 기반 인증**: 단순 숫자로 노출 위험이 있지만 변경 가능성이 낮아 인증 및 연산 시 성능 면에서 유리함

**선택한 방법:**

- **AuthService에서는 Email을 사용하여 인증 진행**
- **JWT 내에서 PK 값을 포함하여 내부적으로 PK를 활용** (유일성과 성능을 고려한 설계)


