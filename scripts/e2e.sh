#!/usr/bin/env bash
# End-to-end smoke test against the running docker stack.
# Walks every controller; assumes single-session login enforcement,
# so it logs each actor in/out instead of holding two tokens at once.
# Usage: ./scripts/e2e.sh [BASE_URL]

BASE="${1:-http://localhost:8080}"
TS=$(python3 -c 'import time;print(int(time.time()))')
A_EMAIL="alice.${TS}@e.com"
B_EMAIL="bob.${TS}@e.com"
PW="Aa12345!"
PASS=0; FAIL=0
declare -a FAILS=()

GREEN=$(printf '\033[32m'); RED=$(printf '\033[31m'); YEL=$(printf '\033[33m'); RST=$(printf '\033[0m')

# All progress output goes to stderr so callers can pipe stdout (response body)
# into files or jq_get without polluting it with PASS/FAIL lines.
section() { printf "\n%s== %s ==%s\n" "$YEL" "$1" "$RST" >&2; }

check() {
  local label="$1" expect="$2" code="$3" body="$4"
  if [[ "$code" =~ ^$expect$ ]]; then
    printf "  %sPASS%s %-50s [%s]\n" "$GREEN" "$RST" "$label" "$code" >&2
    PASS=$((PASS+1))
  else
    printf "  %sFAIL%s %-50s [got %s, want ~%s] body=%s\n" "$RED" "$RST" "$label" "$code" "$expect" "$body" >&2
    FAIL=$((FAIL+1))
    FAILS+=("$label")
  fi
}

# req <label> <expect-regex> <method> <path> [token] [json-body]
req() {
  local label="$1" expect="$2" method="$3" path="$4" tok="${5:-}" body="${6:-}"
  local hdr=()
  local data_args=()
  [[ -n "$tok"  ]] && hdr+=(-H "Authorization: Bearer $tok")
  [[ -n "$body" ]] && { hdr+=(-H "Content-Type: application/json"); data_args=(--data "$body"); }
  local out
  out=$(curl -s -o /tmp/e2e.body -w "%{http_code}" -X "$method" "${hdr[@]}" "${data_args[@]}" "$BASE$path")
  local snippet
  snippet=$(head -c 160 /tmp/e2e.body | tr '\n' ' ')
  check "$label" "$expect" "$out" "$snippet"
  cat /tmp/e2e.body
}

# Read a dotted-path from JSON on stdin.
# Inline -c (not heredoc) so the caller's `< file` redirect still owns stdin.
jq_get() {
  python3 -c 'import json,sys
d=json.load(sys.stdin)
for k in sys.argv[1].split("."):
    d = d[int(k)] if k.isdigit() else d[k]
print(d)' "$1"
}

login() {  # echoes accessToken (or empty on failure)
  local email="$1" pw="$2"
  curl -s -X POST "$BASE/auth/login" -H "Content-Type: application/json" \
    -d "{\"email\":\"$email\",\"password\":\"$pw\"}" \
    | python3 -c "import json,sys
try: print(json.load(sys.stdin)['data']['accessToken'])
except Exception: print('')"
}

logout() { # silent best-effort
  local tok="$1"
  [[ -z "$tok" ]] && return 0
  curl -s -X POST "$BASE/auth/logout" -H "Authorization: Bearer $tok" >/dev/null
}

# Reset the jwt_token table so the single-session enforcement
# doesn't poison the test (the app's AuthService blocks any login
# while ANY other user holds a token).
db_truncate_tokens() {
  docker compose exec -T mysql mysql -uroot -p1234 newsfeed -e "DELETE FROM jwt_token;" 2>/dev/null \
    || echo "  (warn) could not clear jwt_token via docker exec"
}

# ---------- static / public ----------
section "static / public"
db_truncate_tokens
req "GET /"                "200" GET "/"                                  >/dev/null
req "GET /css/style.css"   "200" GET "/css/style.css"                     >/dev/null
req "GET /js/main.js"      "200" GET "/js/main.js"                        >/dev/null
req "GET /feeds/likecount" "200" GET "/feeds/likecount?page=0&size=5"     >/dev/null

# ---------- signup ----------
section "member / signup"
req "signup alice"   "200" POST "/members/signup" "" \
  "{\"name\":\"Alice\",\"email\":\"$A_EMAIL\",\"password\":\"$PW\",\"phoneNumber\":\"010-1111-2222\",\"address\":\"서울\",\"age\":25,\"role\":\"USER\"}" >/tmp/alice.json
A_ID=$(jq_get data.id </tmp/alice.json)
req "signup bob"     "200" POST "/members/signup" "" \
  "{\"name\":\"Bob\",\"email\":\"$B_EMAIL\",\"password\":\"$PW\",\"phoneNumber\":\"010-3333-4444\",\"address\":\"서울\",\"age\":27,\"role\":\"USER\"}" >/tmp/bob.json
B_ID=$(jq_get data.id </tmp/bob.json)
req "signup duplicate email"  "400" POST "/members/signup" "" \
  "{\"name\":\"Dup\",\"email\":\"$A_EMAIL\",\"password\":\"$PW\",\"phoneNumber\":\"010-5\",\"address\":\"x\",\"age\":1,\"role\":\"USER\"}" >/dev/null
req "signup missing role"     "400" POST "/members/signup" "" \
  "{\"name\":\"X\",\"email\":\"missing-role.${TS}@e.com\",\"password\":\"$PW\",\"phoneNumber\":\"010-9\",\"address\":\"x\",\"age\":1}" >/dev/null
req "signup invalid email (no @)" "400" POST "/members/signup" "" \
  "{\"name\":\"X\",\"email\":\"not-an-email\",\"password\":\"$PW\",\"phoneNumber\":\"010-9\",\"address\":\"x\",\"age\":1,\"role\":\"USER\"}" >/dev/null
req "signup invalid email (no tld)" "400" POST "/members/signup" "" \
  "{\"name\":\"X\",\"email\":\"foo@bar\",\"password\":\"$PW\",\"phoneNumber\":\"010-9\",\"address\":\"x\",\"age\":1,\"role\":\"USER\"}" >/dev/null
req "signup invalid email (spaces)" "400" POST "/members/signup" "" \
  "{\"name\":\"X\",\"email\":\"a b@e.com\",\"password\":\"$PW\",\"phoneNumber\":\"010-9\",\"address\":\"x\",\"age\":1,\"role\":\"USER\"}" >/dev/null
req "signup weak pw (4 chars)" "400" POST "/members/signup" "" \
  "{\"name\":\"X\",\"email\":\"weak.${TS}@e.com\",\"password\":\"Aa1!\",\"phoneNumber\":\"010-9\",\"address\":\"x\",\"age\":1,\"role\":\"USER\"}" >/dev/null
req "signup weak pw (no special)" "400" POST "/members/signup" "" \
  "{\"name\":\"X\",\"email\":\"nospec.${TS}@e.com\",\"password\":\"Aaaaaa12\",\"phoneNumber\":\"010-9\",\"address\":\"x\",\"age\":1,\"role\":\"USER\"}" >/dev/null
echo "  Alice id=$A_ID  Bob id=$B_ID"

# ---------- alice session: profile, password, refresh, upload, feed, like ----------
section "alice / login + profile"
db_truncate_tokens
A_TOK=$(login "$A_EMAIL" "$PW"); check "login alice token issued" "200" "$([ -n "$A_TOK" ] && echo 200 || echo 400)" "$A_TOK"
req "login wrong pw"  "400|401|403" POST "/auth/login" "" "{\"email\":\"$A_EMAIL\",\"password\":\"WRONG\"}" >/dev/null
req "GET /members/{aliceId}" "200" GET "/members/$A_ID"        >/dev/null
req "PUT /members/update"    "200" PUT "/members/update" "$A_TOK" \
  "{\"password\":\"$PW\",\"name\":\"AliceX\",\"phoneNumber\":\"010-9999-0000\",\"address\":\"부산\",\"image\":\"\"}" >/dev/null
req "PUT /members/password"  "200" PUT "/members/password" "$A_TOK" \
  "{\"oldPassword\":\"$PW\",\"newPassword\":\"$PW!new\"}" >/dev/null
logout "$A_TOK"
db_truncate_tokens
A_TOK=$(login "$A_EMAIL" "$PW!new"); check "login w/ new pw" "200" "$([ -n "$A_TOK" ] && echo 200 || echo 400)" ""
req "PUT password back"      "200" PUT "/members/password" "$A_TOK" \
  "{\"oldPassword\":\"$PW!new\",\"newPassword\":\"$PW\"}" >/dev/null
logout "$A_TOK"
db_truncate_tokens
A_TOK=$(login "$A_EMAIL" "$PW"); check "login w/ orig pw" "200" "$([ -n "$A_TOK" ] && echo 200 || echo 400)" ""

section "file upload (alice)"
printf '\x89PNG\r\n\x1a\n\x00\x00\x00\rIHDR\x00\x00\x00\x01\x00\x00\x00\x01\x08\x06\x00\x00\x00\x1f\x15\xc4\x89\x00\x00\x00\rIDATx\x9cc\xfa\xcf\x00\x00\x00\x02\x00\x01\xe5\x27\xde\xfc\x00\x00\x00\x00IEND\xaeB`\x82' > /tmp/e2e.png
out=$(curl -s -o /tmp/e2e.body -w "%{http_code}" -X POST "$BASE/files/image" \
  -H "Authorization: Bearer $A_TOK" -F "file=@/tmp/e2e.png;type=image/png")
check "upload PNG" "200" "$out" "$(head -c 120 /tmp/e2e.body)"
IMG_URL=$(jq_get data.url </tmp/e2e.body 2>/dev/null)
echo "  IMG_URL=$IMG_URL"
echo '<svg xmlns="http://www.w3.org/2000/svg" />' > /tmp/e2e.svg
out=$(curl -s -o /tmp/e2e.body -w "%{http_code}" -X POST "$BASE/files/image" \
  -H "Authorization: Bearer $A_TOK" -F "file=@/tmp/e2e.svg;type=image/svg+xml")
check "upload non-image rejected" "400" "$out" "$(head -c 120 /tmp/e2e.body)"
out=$(curl -s -o /tmp/e2e.body -w "%{http_code}" -X POST "$BASE/files/image" \
  -F "file=@/tmp/e2e.png;type=image/png")
check "upload without auth" "401|403" "$out" ""
req "GET uploaded image"   "200" GET "$IMG_URL" >/dev/null

section "alice / feed + like"
req "alice creates feed1"  "200" POST "/feeds" "$A_TOK" \
  "{\"title\":\"Hello\",\"content\":\"first post\",\"image\":\"$IMG_URL\",\"address\":\"서울특별시\",\"latitude\":37.5,\"longitude\":127.0}" >/tmp/feed1.json
req "alice creates feed2"  "200" POST "/feeds" "$A_TOK" \
  "{\"title\":\"Second\",\"content\":\"second\",\"image\":\"$IMG_URL\",\"address\":\"서울\",\"latitude\":37.5,\"longitude\":127.0}" >/dev/null
LCJSON=$(curl -s "$BASE/feeds/likecount?page=0&size=20")
FEED_ID=$(python3 -c "import json,sys;d=json.loads(sys.argv[1])['data']['content'];print(d[0]['feedId'] if d else 0)" "$LCJSON")
echo "  (newest feedId=$FEED_ID)"
req "GET feed detail"        "200" GET "/feeds/$FEED_ID"     >/dev/null
req "GET feeds by member"    "200" GET "/feeds/members/$A_ID" >/dev/null
req "PATCH update feed"      "200" PATCH "/feeds/$FEED_ID" "$A_TOK" \
  "{\"title\":\"Hello-updated\",\"content\":\"updated\"}" >/dev/null
req "feed create w/o auth"   "401|403|400" POST "/feeds" "" \
  "{\"title\":\"x\",\"content\":\"x\",\"image\":\"$IMG_URL\",\"address\":\"x\",\"latitude\":0,\"longitude\":0}" >/dev/null

req "POST like"          "200" POST "/likes/like/$FEED_ID"    "$A_TOK" "" >/dev/null
req "GET like count"     "200" GET "/likes/$FEED_ID" "$A_TOK"      >/dev/null
req "POST dislike"       "200" POST "/likes/dislike/$FEED_ID" "$A_TOK" "" >/dev/null
req "POST like no auth"  "401|403" POST "/likes/like/$FEED_ID" "" "" >/dev/null

# ---------- alice -> bob friendship + bob's comment ----------
section "alice -> bob friend request"
req "alice -> bob request" "200" POST "/friends" "$A_TOK" "{\"receiverId\":$B_ID}" >/tmp/friend.json
FRIEND_PK=$(python3 -c "import json,sys
try:
    d=json.load(sys.stdin)
    print(d.get('id') or (d.get('data') or {}).get('id') or '')
except Exception:
    print('')" </tmp/friend.json)
echo "  (friend pk=$FRIEND_PK)" >&2
req "alice sent list"      "200" GET "/friends/sent?page=0&size=5" "$A_TOK" >/dev/null
logout "$A_TOK"; db_truncate_tokens

section "bob / login + accept + comment"
B_TOK=$(login "$B_EMAIL" "$PW"); check "login bob" "200" "$([ -n "$B_TOK" ] && echo 200 || echo 400)" ""
req "bob received list" "200" GET "/friends/received?page=0&size=5" "$B_TOK" >/dev/null
req "bob accepts"       "200" PATCH "/friends/accept" "$B_TOK" "{\"senderId\":$A_ID}" >/dev/null
req "bob friend list"   "200" GET "/friends?page=0&size=5" "$B_TOK" >/dev/null

req "POST comment (bob)" "200" POST "/comments" "$B_TOK" \
  "{\"memberId\":$B_ID,\"feedId\":$FEED_ID,\"parentId\":null,\"content\":\"멋져요\"}" >/tmp/cm.json
CID=$(jq_get data.commentId </tmp/cm.json)
req "PATCH comment (bob)" "200" PATCH "/comments/$CID" "$B_TOK" \
  "{\"memberId\":$B_ID,\"feedId\":$FEED_ID,\"parentId\":null,\"content\":\"수정됨\"}" >/dev/null
req "GET single comment"   "200" GET "/comments/$CID"            >/dev/null
req "GET comments by feed" "200" GET "/comments/feed/$FEED_ID"   >/dev/null
logout "$B_TOK"; db_truncate_tokens

# ---------- alice child-comment + cleanup ----------
section "alice / child-comment + cleanup"
A_TOK=$(login "$A_EMAIL" "$PW"); check "re-login alice" "200" "$([ -n "$A_TOK" ] && echo 200 || echo 400)" ""
req "POST child-comment (alice)" "200" POST "/comments/child-comments" "$A_TOK" \
  "{\"memberId\":$A_ID,\"feedId\":$FEED_ID,\"parentId\":$CID,\"content\":\"감사합니다\"}" >/dev/null

req "DELETE feed"   "200|204" DELETE "/feeds/$FEED_ID" "$A_TOK" >/dev/null
# FriendResponseDto doesn't expose the Friend PK; look it up directly.
FRIEND_PK=$(docker compose exec -T mysql mysql -uroot -p1234 newsfeed -N -e \
  "SELECT id FROM friend WHERE sender_id=$A_ID AND receiver_id=$B_ID ORDER BY id DESC LIMIT 1;" 2>/dev/null | tail -1)
echo "  (friend pk resolved=$FRIEND_PK)" >&2
req "alice deletes friendship" "200|204" DELETE "/friends/${FRIEND_PK:-0}" "$A_TOK" >/dev/null
req "logout alice"  "200" POST "/auth/logout" "$A_TOK" "" >/dev/null

# ---------- refresh token / reissue ----------
section "auth tokens"
db_truncate_tokens
A_TOK_BODY=$(curl -s -X POST "$BASE/auth/login" -H "Content-Type: application/json" -d "{\"email\":\"$A_EMAIL\",\"password\":\"$PW\"}")
A_RT=$(echo "$A_TOK_BODY" | python3 -c "import json,sys;print(json.load(sys.stdin)['data']['refreshToken'])")
A_TOK=$(echo "$A_TOK_BODY" | python3 -c "import json,sys;print(json.load(sys.stdin)['data']['accessToken'])")
req "POST /auth/refresh"  "200" POST "/auth/refresh" "" "{\"refreshToken\":\"$A_RT\"}" >/dev/null
out=$(curl -s -o /tmp/e2e.body -w "%{http_code}" -X POST "$BASE/auth/reissue?email=$A_EMAIL&password=$PW")
check "POST /auth/reissue (dev)" "200|400" "$out" "$(head -c 120 /tmp/e2e.body)"
logout "$A_TOK"

# ---------- summary ----------
echo
echo "==============================="
printf "PASS: %s%d%s   FAIL: %s%d%s\n" "$GREEN" "$PASS" "$RST" "$RED" "$FAIL" "$RST"
if [[ $FAIL -gt 0 ]]; then
  printf "Failed:\n"; printf "  - %s\n" "${FAILS[@]}"
  exit 1
fi
exit 0
