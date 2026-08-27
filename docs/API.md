# NewsFeed API 문서

> 실제 컨트롤러 매핑 기준으로 작성. Base URL: `http://localhost:8080`

## 공통 규약

### 응답 봉투 (CommonResponse)
대부분의 엔드포인트는 다음 형태로 응답한다. (일부 `/friends`, `/kakao`, `/auth/refresh`는 원시 형태)

```json
{ "message": "설명", "data": { /* 페이로드 */ } }
```

- 프런트(`api.js`)는 `{message, data}` 형태면 `data`만 언래핑한다.
- `Page<T>` 데이터는 Spring Data 형식(`content`, `totalElements`, `last`, `number` …).

### 인증
- 로그인 후 발급된 **Access Token**을 헤더에 담아 호출한다.
  `Authorization: Bearer <accessToken>`
- **JWT claims 기반 stateless** 인증(요청당 DB 조회 없음). 만료 시 `401` → `/auth/refresh`로 재발급.
- 아래 표의 **Auth** 열: `Y`=토큰 필수, `N`=공개, `옵션`=없어도 되나 있으면 개인화(예: `likedByMe`).

### 에러 코드
| 상태 | 의미 |
|---|---|
| `400` | 잘못된 요청 / 검증 실패(`IllegalArgument`·`IllegalState`), 응답 `{message, fieldErrors}` 가능 |
| `401` | 토큰 없음/만료/무효 (→ refresh) |
| `403` | 권한 없음 |
| `404` | 리소스 없음 |
| `500` | 서버 오류(내부 메시지 미노출) |

---

## 🔐 인증 (`/auth`, `/kakao`)

| Method | Path | Auth | 설명 | Body |
|---|---|---|---|---|
| POST | `/auth/login` | N | 로그인 → access/refresh 발급 | `{ email, password }` |
| POST | `/auth/refresh` | N | refresh로 access 재발급 | `{ refreshToken }` |
| POST | `/auth/logout` | Y | 로그아웃(토큰 무효화) | — (헤더 토큰) |
| POST | `/auth/reissue` | N | 이메일/비번으로 access 재발급 | `?email=&password=` |
| GET | `/kakao/authorize` | N | 카카오 인가 페이지로 302 리다이렉트 | — |
| GET | `/callback?code=` | N | 카카오 code 콜백 → JWT 발급 부트스트랩 HTML | — |
| POST | `/kakao/login` | N | 카카오 AccessToken → 앱 JWT | 헤더 `Authorization: Bearer <kakao token>` |
| POST | `/kakao/logout` | N | 카카오 로그아웃 | 헤더 kakao token |

**로그인 응답 예**
```json
{ "message": "로그인 성공", "data": { "id": 130, "accessToken": "ey...", "refreshToken": "ey..." } }
```

---

## 👤 회원 (`/members`)

| Method | Path | Auth | 설명 | Body/Query |
|---|---|---|---|---|
| POST | `/members/signup` | N | 회원가입 | `{ name, email, password, phoneNumber, address, age, role:"USER" }` |
| GET | `/members/{id}` | N | 프로필 조회(`id`,`name`,`email`,`image`,`isPrivate`) | — |
| GET | `/members/search?q=&page=&size=` | Y | 이름/이메일 검색 | — |
| PUT | `/members/update` | Y | 회원정보 수정 | `{ id, password, name, phoneNumber, address, image }` |
| PUT | `/members/password` | Y | 비밀번호 변경 | `{ oldPassword, newPassword }` |
| PATCH | `/members/privacy` | Y | 비공개 계정 토글 → `{ isPrivate }` | — |
| DELETE | `/members/delete` | Y | 회원 탈퇴(Soft Delete) | `{ password }` |

---

## 📝 피드 (`/feeds`)

| Method | Path | Auth | 설명 | Body/Query |
|---|---|---|---|---|
| POST | `/feeds` | Y | 게시물 작성(다중 이미지·위치 선택) | `{ title, content, image, images:[], address, latitude, longitude }` |
| GET | `/feeds/{feedId}` | N | 상세(작성자·댓글·좋아요·`images`) | — |
| GET | `/feeds/latest?page=&size=` | N | 최신순 | — |
| GET | `/feeds/likecount?page=&size=` | N | 인기순(좋아요) — 탐색에도 사용 | — |
| GET | `/feeds/following?page=&size=` | Y | 팔로우(친구) 피드, 차단 회원 제외 | — |
| GET | `/feeds/search?q=&page=&size=` | N | 제목/내용/#해시태그 검색 | — |
| GET | `/feeds/members/{memberId}` | 옵션 | 회원 게시물 그리드(비공개면 친구만) | — |
| PATCH | `/feeds/{feedId}` | Y | 수정(제목/내용/`images` 교체) — 작성자만 | `{ title, content, images:[] }` |
| DELETE | `/feeds/{feedId}` | Y | 삭제 — 작성자만 | — |

> 위치: `images`가 있으면 커버(`image`)=첫 장. 좌표 없고 주소만 있으면 카카오 지오코딩으로 보완, 둘 다 없으면 위치 없이 게시.

---

## ❤️ 좋아요 (`/likes`, `/comment-likes`)

| Method | Path | Auth | 설명 |
|---|---|---|---|
| POST | `/likes/like/{feedId}` | Y | 좋아요(per-user, 멱등) → `{ feedId, likeCount, likedByMe:true }` |
| POST | `/likes/dislike/{feedId}` | Y | 좋아요 취소 → `{ …, likedByMe:false }` |
| GET | `/likes/{feedId}` | 옵션 | 좋아요 수 + 내 좋아요 여부(`likedByMe`) |
| GET | `/likes/{feedId}/members` | N | 좋아요한 회원 목록(`[{id,name,image}]`) |
| POST | `/comment-likes/{commentId}` | Y | 댓글 좋아요 토글 → `{ commentId, liked, count }` |

---

## 💬 댓글 (`/comments`)

| Method | Path | Auth | 설명 | Body |
|---|---|---|---|---|
| POST | `/comments` | Y | 댓글/답글 작성(`parentId`로 답글) | `{ memberId, feedId, parentId, content }` |
| POST | `/comments/child-comments` | Y | 대댓글 작성(대체 경로) | `{ parentId, content, … }` |
| GET | `/comments/feed/{feedId}` | 옵션 | 피드 댓글(대댓글·`likeCount`·`likedByMe` 포함) | — |
| GET | `/comments/{commentId}` | N | 단일 댓글 | — |
| PATCH | `/comments/{commentId}` | Y | 댓글 수정 | `{ content }` |
| DELETE | `/comments/{commentId}` | Y | 댓글 삭제 — 작성자만 | — |

---

## 👥 친구/팔로우 (`/friends`)

| Method | Path | Auth | 설명 | Body |
|---|---|---|---|---|
| POST | `/friends` | Y | 친구 요청 | `{ receiverId }` |
| PATCH | `/friends/accept` | Y | 요청 수락 | `{ senderId }` |
| GET | `/friends?page=&size=` | Y | 내 친구 목록(상대 memberId·실명) | — |
| GET | `/friends/received?page=&size=` | Y | 받은 요청(발신자) | — |
| GET | `/friends/sent?page=&size=` | Y | 보낸 요청(수신자) | — |
| DELETE | `/friends/{friendId}` | Y | 친구 삭제(Friend PK) | — |
| DELETE | `/friends/by-member/{memberId}` | Y | 회원 기준 친구 해제(방향 무관) | — |
| GET | `/friends/status/{memberId}` | Y | 관계 상태 `self|friends|requested_by_me|requested_to_me|none` | — |
| GET | `/friends/count/{memberId}` | N | 친구 수(팔로워=팔로잉) → `{ friends }` | — |
| GET | `/friends/members/{memberId}` | N | 친구(팔로워/팔로잉) 목록 | — |
| GET | `/friends/suggestions?limit=` | Y | 팔로우 추천(공통 친구 수 순) | — |

---

## 💬 DM (`/messages`)

| Method | Path | Auth | 설명 | Body/Query |
|---|---|---|---|---|
| POST | `/messages` | Y | 메시지 전송(내용=텍스트 또는 업로드 이미지 URL) | `{ receiverId, content }` |
| GET | `/messages/with/{peerId}?page=&size=` | Y | 대화 조회(createdAt ASC) | — |
| GET | `/messages/unread?page=&size=` | Y | 안 읽은 메시지 | — |
| GET | `/messages/inbox?page=&size=` | Y | 받은 메시지 | — |
| PATCH | `/messages/{id}/read` | Y | 읽음 처리(발신자에게 실시간 푸시) | — |
| DELETE | `/messages/with/{peerId}` | Y | 대화 전체 삭제 | — |

**실시간(STOMP over WebSocket)**: `ws://<host>/ws?token=<accessToken>` 로 연결 후 구독
- `/user/queue/messages` — 새 메시지
- `/user/queue/notifications` — 새 알림
- `/user/queue/read` — 읽음 이벤트 `{ messageId, peerId }`

---

## 🔔 알림 (`/notifications`)

| Method | Path | Auth | 설명 |
|---|---|---|---|
| GET | `/notifications?page=&size=` | Y | 알림 목록(`type`, `actorName`, `feedId`, `message`, `readStatus`, `createdAt`) |
| GET | `/notifications/unread-count` | Y | 안 읽은 알림 수 → `{ count }` |
| PATCH | `/notifications/read-all` | Y | 모두 읽음 처리 |

> type: `LIKE` · `COMMENT`(답글 포함) · `FRIEND_REQUEST` · `FRIEND_ACCEPT`

---

## 🔖 저장 · 🛡️ 차단 · 신고

| Method | Path | Auth | 설명 |
|---|---|---|---|
| POST | `/bookmarks/{feedId}` | Y | 저장 토글 → `{ feedId, bookmarked }` |
| GET | `/bookmarks/{feedId}` | 옵션 | 저장 여부 |
| GET | `/bookmarks?page=&size=` | Y | 내가 저장한 게시물(피드 카드) |
| POST | `/blocks/{memberId}` | Y | 차단 토글 → `{ memberId, blocked }` |
| GET | `/blocks/{memberId}` | 옵션 | 차단 여부 |
| GET | `/blocks` | Y | 내가 차단한 목록 |
| POST | `/reports` | Y | 신고 접수 | `{ targetType, targetId, reason }` |

---

## 📎 파일 (`/files`)

| Method | Path | Auth | 설명 |
|---|---|---|---|
| POST | `/files/image` | Y | 이미지 업로드(multipart `file`) → `{ url }`. 허용: jpeg/png/gif/webp |

업로드된 이미지는 `/uploads/YYYY/MM/<uuid>.<ext>` 로 정적 서빙. 프런트는 업로드 전 최대 1080px로 리사이즈.

---

## 호출 예시

```bash
# 로그인
TOKEN=$(curl -s -X POST http://localhost:8080/auth/login \
  -H "Content-Type: application/json" \
  -d '{"email":"alice@test.com","password":"Alice123!@#"}' \
  | python3 -c "import sys,json;print(json.load(sys.stdin)['data']['accessToken'])")

# 최신 피드
curl -s "http://localhost:8080/feeds/latest?page=0&size=10"

# 좋아요
curl -s -X POST "http://localhost:8080/likes/like/48" -H "Authorization: Bearer $TOKEN"

# 게시물 검색(해시태그)
curl -s "http://localhost:8080/feeds/search?q=%23여행"
```
