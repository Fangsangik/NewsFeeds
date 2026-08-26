import { api } from "./api.js";
import { auth } from "./store.js";
import { el, avatar, toast, fmtTimeAgo, escapeHtml } from "./ui.js";

// ---------- shared STOMP client (one per browser tab) ----------
let stompClient = null;
const incomingHandlers = new Set();

export function connectStomp() {
  if (!auth.isLoggedIn) return;
  // 클라이언트가 이미 있으면(연결 중 포함) 재생성하지 않는다 — 중복 구독/중복 수신 방지.
  if (stompClient) return;
  if (typeof StompJs === "undefined") {
    console.warn("StompJs lib not loaded");
    return;
  }

  // Raw WebSocket URL so the ?token=... query param survives the upgrade.
  const wsProto = location.protocol === "https:" ? "wss" : "ws";
  const wsUrl = `${wsProto}://${location.host}/ws?token=${encodeURIComponent(auth.accessToken)}`;

  stompClient = new StompJs.Client({
    brokerURL: wsUrl,
    reconnectDelay: 5000,
    debug: () => {},
  });

  stompClient.onConnect = () => {
    stompClient.subscribe("/user/queue/messages", (frame) => {
      try {
        const payload = JSON.parse(frame.body);
        incomingHandlers.forEach(h => { try { h(payload); } catch {} });
      } catch (e) {
        console.warn("bad STOMP frame", e);
      }
    });
    // 실시간 알림: 도착 시 전역 이벤트 발행 → 토픽바 🔔 뱃지가 갱신된다.
    stompClient.subscribe("/user/queue/notifications", (frame) => {
      try {
        const payload = JSON.parse(frame.body);
        window.dispatchEvent(new CustomEvent("nf:notification", { detail: payload }));
      } catch (e) {
        console.warn("bad notification frame", e);
      }
    });
    // 읽음 실시간: 상대가 내 메시지를 읽으면 도착 → 대화창이 '읽음' 표시를 갱신한다.
    stompClient.subscribe("/user/queue/read", (frame) => {
      try {
        const payload = JSON.parse(frame.body);
        window.dispatchEvent(new CustomEvent("nf:read", { detail: payload }));
      } catch (e) {
        console.warn("bad read frame", e);
      }
    });
  };
  stompClient.onStompError = (frame) => console.warn("STOMP error", frame.headers?.message);
  stompClient.activate();
}

export function disconnectStomp() {
  if (stompClient) { stompClient.deactivate(); stompClient = null; }
  incomingHandlers.clear();
}

function onIncoming(handler) {
  incomingHandlers.add(handler);
  return () => incomingHandlers.delete(handler);
}

// ---------- views ----------
export async function renderDm(root, peerId) {
  if (!auth.isLoggedIn) { location.hash = "#/login"; return; }
  if (peerId) return renderConversation(root, Number(peerId));
  return renderInbox(root);
}

async function renderInbox(root) {
  root.innerHTML = "";
  root.appendChild(el("div", { class: "center muted", style: { padding: "40px" } }, [
    el("span", { class: "spinner" }),
  ]));

  // Failures fall through to the empty state — DM inbox shouldn't bail with
  // a scary error just because /friends returned 401/5xx; treat unknown
  // results the same as "no friends yet".
  let friends = [];
  let loadError = null;
  try {
    const page = await api.get("/friends?page=0&size=50");
    friends = Array.isArray(page) ? page : (page?.content ?? []);
  } catch (e) {
    console.warn("[dm] /friends failed:", e);
    loadError = e;
  }

  root.innerHTML = "";
  const header = el("div", { class: "dm-header" }, [
    el("h2", {}, "메시지"),
    el("div", { class: "muted" }, friends.length
      ? "대화할 친구를 선택하세요"
      : "아직 대화할 친구가 없어요"),
  ]);
  const list = el("div", { class: "dm-list" });

  if (friends.length) {
    friends.forEach(f => {
      const peer = pickPeer(f);
      if (!peer.id) return;
      const sub = el("div", { class: "sub muted" }, "대화 시작하기");
      const unreadDot = el("span", { class: "dm-unread-dot", style: { display: "none" } }, "");
      const row = el("button", { class: "dm-list-row", onclick: () => { location.hash = `#/dm/${peer.id}`; } }, [
        avatar(peer.name || `user${peer.id}`, "sm", peer.image),
        el("div", { class: "dm-list-meta" }, [
          el("div", { class: "name" }, peer.name || `user${peer.id}`),
          sub,
        ]),
        unreadDot,
      ]);
      list.appendChild(row);
      // 마지막 메시지 미리보기 + 안읽음 여부. 대화는 createdAt ASC라 마지막 원소가 최신이다.
      api.get(`/messages/with/${peer.id}?page=0&size=30`).then(page => {
        const arr = page?.content ?? [];
        const last = arr[arr.length - 1];
        if (last?.message) sub.textContent = (Number(last.senderId) === Number(auth.meId) ? "나: " : "") + last.message;
        // 상대가 보낸 안 읽은 메시지가 있으면 미읽음 점 표시 + 목록 최상단으로 이동
        const hasUnread = arr.some(m => Number(m.receiverId) === Number(auth.meId) && m.readStatus === false);
        if (hasUnread) {
          unreadDot.style.display = "block";
          row.classList.add("unread");
          list.insertBefore(row, list.firstChild);
        }
      }).catch(() => {});
    });
  } else {
    list.appendChild(emptyFriendsBlock(loadError));
  }
  root.appendChild(el("section", { class: "dm-wrap" }, [header, list]));
}

function emptyFriendsBlock(loadError) {
  // Both "user has no friends yet" and "we couldn't reach /friends" land here.
  // We only mention the real error in a faint secondary line so the page
  // still feels intentional.
  const block = el("div", { class: "empty", style: { padding: "60px 20px" } }, [
    el("div", { style: { fontSize: "44px", marginBottom: "12px" } }, "💬"),
    el("div", { style: { fontWeight: "600", marginBottom: "6px" } }, "아직 친구가 없어요"),
    el("div", { class: "muted", style: { marginBottom: "14px" } },
      "친구를 추가하면 여기서 바로 대화를 시작할 수 있어요."),
  ]);
  if (loadError) {
    block.appendChild(el("div", { class: "muted", style: { fontSize: "12px", marginTop: "10px" } },
      `(목록을 잠시 가져오지 못했어요 — 새로고침 후 다시 시도해주세요)`));
  }
  return block;
}

// FriendListDto shape varies; try common field names to find the *other* user.
function pickPeer(friend) {
  const me = Number(auth.meId);
  // Try direct fields first
  const candidates = [
    { id: friend.peerId, name: friend.peerName, image: friend.peerImage },
    { id: friend.friendId, name: friend.friendName, image: friend.friendImage },
    { id: friend.memberId, name: friend.memberName, image: friend.memberImage },
    { id: friend.id, name: friend.name, image: friend.image },
  ];
  // Sender/receiver style: pick the one that isn't me
  if (friend.senderId != null && friend.receiverId != null) {
    const isSenderMe = Number(friend.senderId) === me;
    candidates.unshift({
      id: isSenderMe ? friend.receiverId : friend.senderId,
      name: isSenderMe ? (friend.receiverName || friend.name) : (friend.senderName || friend.name),
    });
  }
  for (const c of candidates) {
    if (c.id != null && Number(c.id) !== me) return { id: Number(c.id), name: c.name, image: c.image };
  }
  // Last resort: any non-me id we can spot
  for (const [k, v] of Object.entries(friend || {})) {
    if (k.toLowerCase().endsWith("id") && typeof v === "number" && v !== me) {
      return { id: v, name: friend.name || friend.peerName || `user${v}` };
    }
  }
  return { id: null };
}

async function renderConversation(root, peerId) {
  root.innerHTML = "";
  root.appendChild(el("div", { class: "center muted", style: { padding: "40px" } }, [el("span", { class: "spinner" })]));

  let peer = null, page = null;
  try {
    [peer, page] = await Promise.all([
      api.get(`/members/${peerId}`).catch(() => null),
      api.get(`/messages/with/${peerId}?page=0&size=100`).catch(() => null),
    ]);
  } catch (e) {
    root.innerHTML = "";
    root.appendChild(el("div", { class: "empty" }, `대화 로딩 실패: ${e.message}`));
    return;
  }
  const peerName = (peer && peer.name) || `user${peerId}`;

  root.innerHTML = "";
  const list = el("div", { class: "dm-thread" });
  const input = el("input", { type: "text", placeholder: "메시지 보내기...", onkeydown: (e) => {
    // e.isComposing: 한글 IME 조합 확정 Enter를 무시(안 하면 전송이 2번 발생).
    if (e.key === "Enter" && !e.shiftKey && !e.isComposing) { e.preventDefault(); send(); }
  }});
  const sendBtn = el("button", { class: "btn-primary dm-send", onclick: send }, "보내기");

  const messages = (page?.content ?? []);
  messages.forEach(m => appendMessage(list, m));
  setTimeout(() => list.scrollTo({ top: list.scrollHeight }), 0);

  // 상대가 보낸 안 읽은 메시지를 읽음 처리 (안읽음 뱃지 정리)
  messages
    .filter(m => Number(m.receiverId) === Number(auth.meId) && m.readStatus === false)
    .forEach(m => api.patch(`/messages/${m.id}/read`).catch(() => {}));

  // Subscribe to live pushes for this peer
  const unsub = onIncoming((m) => {
    if (Number(m.senderId) === Number(peerId) || Number(m.receiverId) === Number(peerId)) {
      if (appendMessage(list, m)) list.scrollTo({ top: list.scrollHeight, behavior: "smooth" });
    } else {
      toast(`💬 새 메시지 (user${m.senderId})`);
    }
  });
  // 상대가 내 메시지를 읽으면 실시간으로 '읽음' 표시. 이 대화 상대가 읽은 경우만.
  const onRead = (e) => {
    const d = e.detail || {};
    if (Number(d.peerId) !== Number(peerId)) return;
    // 해당 메시지 아래에 '읽음' 뱃지 추가(없으면). 특정 mid를 모르면 내 마지막 메시지에 표시.
    let node = d.messageId != null ? list.querySelector(`[data-mid="${d.messageId}"]`) : null;
    if (!node) { const mine = [...list.querySelectorAll(".dm-msg.mine")]; node = mine[mine.length - 1]; }
    if (node && !node.querySelector(".dm-read")) {
      const meta = node.querySelector(".dm-meta");
      if (meta) meta.insertBefore(el("span", { class: "dm-read" }, "읽음"), meta.firstChild);
    }
  };
  window.addEventListener("nf:read", onRead);

  // Detach on hashchange so we don't double-append after navigation
  const cleanup = () => { unsub(); window.removeEventListener("nf:read", onRead); window.removeEventListener("hashchange", cleanup); };
  window.addEventListener("hashchange", cleanup);

  async function send() {
    const text = input.value.trim();
    if (!text) return;
    sendBtn.disabled = true;
    try {
      const dto = await api.post("/messages", { receiverId: Number(peerId), content: text });
      input.value = "";
      appendMessage(list, dto);
      list.scrollTo({ top: list.scrollHeight, behavior: "smooth" });
    } catch (e) {
      toast(e.message || "전송 실패");
    } finally {
      sendBtn.disabled = false;
      input.focus();
    }
  }

  // 사진 전송: 업로드한 이미지 URL을 메시지 내용으로 보낸다. (수신측이 URL이면 이미지로 렌더)
  const photoInput = el("input", { type: "file", accept: "image/*", style: { display: "none" }, onchange: async (e) => {
    const f = e.target.files?.[0]; e.target.value = "";
    if (!f) return;
    photoBtn.disabled = true;
    try {
      const r = await api.uploadImage(f);
      if (!r?.url) throw new Error("업로드 실패");
      const dto = await api.post("/messages", { receiverId: Number(peerId), content: r.url });
      appendMessage(list, dto);
      list.scrollTo({ top: list.scrollHeight, behavior: "smooth" });
    } catch (err) { toast(err.message || "사진 전송 실패"); }
    finally { photoBtn.disabled = false; }
  }});
  const photoBtn = el("button", { class: "dm-photo", title: "사진 보내기", onclick: () => photoInput.click() }, "📷");

  root.appendChild(
    el("section", { class: "dm-conv" }, [
      el("div", { class: "dm-conv-head" }, [
        el("button", { class: "btn-ghost", onclick: () => { location.hash = "#/dm"; } }, "← 목록"),
        avatar(peerName),
        el("div", { class: "name" }, peerName),
        el("button", { class: "btn-ghost dm-conv-del", title: "대화 삭제", onclick: async () => {
          if (!confirm("이 대화를 삭제할까요? (내 화면에서 대화 내용이 사라집니다)")) return;
          try { await api.del(`/messages/with/${peerId}`); toast("대화를 삭제했어요."); location.hash = "#/dm"; }
          catch (err) { toast(err.message || "삭제 실패"); }
        }}, "🗑"),
      ]),
      list,
      el("div", { class: "dm-composer" }, [photoBtn, input, sendBtn, photoInput]),
    ])
  );

  setTimeout(() => input.focus(), 0);
}

function isImageUrl(s) {
  return typeof s === "string" && /^\/uploads\/.*\.(png|jpe?g|gif|webp)$/i.test(s);
}

function messageRow(m) {
  const me = Number(auth.meId);
  const mine = Number(m.senderId) === me;
  const body = isImageUrl(m.message)
    ? el("img", { class: "dm-image", src: m.message, alt: "사진", onclick: () => window.open(m.message, "_blank") })
    : el("div", { class: "dm-bubble" }, m.message || "");
  const meta = [el("span", { class: "dm-time muted" }, fmtTimeAgo(m.createdAt) || "")];
  if (mine && m.readStatus) meta.unshift(el("span", { class: "dm-read" }, "읽음"));
  const node = el("div", { class: `dm-msg ${mine ? "mine" : "theirs"}` }, [
    body,
    el("div", { class: "dm-meta" }, meta),
  ]);
  if (m.id != null) node.dataset.mid = String(m.id);
  return node;
}

// 같은 메시지 id가 이미 있으면 추가하지 않는다 (로컬 append + STOMP 수신 중복 방지).
function appendMessage(list, m) {
  if (m && m.id != null && list.querySelector(`[data-mid="${m.id}"]`)) return false;
  list.appendChild(messageRow(m));
  return true;
}
