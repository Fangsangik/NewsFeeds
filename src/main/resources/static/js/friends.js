import { api } from "./api.js";
import { auth } from "./store.js";
import { el, avatar, toast } from "./ui.js";

const TABS = [
  { key: "friends",  label: "친구" },
  { key: "received", label: "받은 요청" },
  { key: "sent",     label: "보낸 요청" },
  { key: "search",   label: "친구 찾기" },
];

let activeTab = "friends";

export function renderFriends(root) {
  if (!auth.isLoggedIn) { location.hash = "#/login"; return; }
  root.innerHTML = "";

  const tabsEl = el("div", { class: "friends-tabs" }, TABS.map(t =>
    el("button", {
      class: `friends-tab ${activeTab === t.key ? "active" : ""}`,
      onclick: () => { activeTab = t.key; renderFriends(root); },
    }, t.label)
  ));

  const body = el("div", { class: "friends-body" });

  const wrap = el("section", { class: "friends-wrap" }, [
    el("div", { class: "friends-head" }, el("h2", {}, "친구")),
    tabsEl,
    body,
  ]);
  root.appendChild(wrap);

  if (activeTab === "friends") loadFriends(body);
  else if (activeTab === "received") loadReceived(body);
  else if (activeTab === "sent") loadSent(body);
  else loadSearch(body);
}

// ---------- friends list ----------
async function loadFriends(body) {
  body.innerHTML = '<div class="muted center" style="padding:24px">불러오는 중...</div>';
  try {
    const page = await api.get("/friends?page=0&size=50");
    const items = page?.content ?? [];
    body.innerHTML = "";
    if (!items.length) {
      body.appendChild(el("div", { class: "empty" }, "아직 친구가 없어요. 친구 찾기에서 추가해보세요."));
      return;
    }
    items.forEach(f => body.appendChild(friendRow(f)));
  } catch (e) {
    console.warn("[friends] list failed:", e);
    body.innerHTML = "";
    body.appendChild(el("div", { class: "empty" }, "친구 목록을 가져오지 못했어요."));
  }
}

function friendRow(f) {
  const name = f.name || `user${f.id}`;
  const goProfile = () => { location.hash = `#/profile/${f.id}`; };
  const av = avatar(name);
  av.style.cursor = "pointer";
  av.addEventListener("click", goProfile);
  return el("div", { class: "friend-card" }, [
    av,
    el("div", { class: "friend-meta clickable", style: { cursor: "pointer" }, onclick: goProfile }, [
      el("div", { class: "name" }, name),
      el("div", { class: "sub muted" }, "프로필 보기"),
    ]),
    el("button", { class: "btn-ghost", onclick: () => { location.hash = `#/dm/${f.id}`; } }, "메시지"),
  ]);
}

// ---------- received requests ----------
async function loadReceived(body) {
  body.innerHTML = '<div class="muted center" style="padding:24px">불러오는 중...</div>';
  try {
    const page = await api.get("/friends/received?page=0&size=50");
    const items = page?.content ?? [];
    body.innerHTML = "";
    if (!items.length) {
      body.appendChild(el("div", { class: "empty" }, "받은 친구 요청이 없어요."));
      return;
    }
    items.forEach(r => body.appendChild(receivedRow(r, body)));
  } catch (e) {
    body.innerHTML = "";
    body.appendChild(el("div", { class: "empty" }, "받은 요청을 가져오지 못했어요."));
  }
}

function receivedRow(r, body) {
  // FriendRequestResponseDto = { receiverEmail, receiverName } — but on /received the values
  // actually represent the *sender* of the request (DTO naming is a touch off).
  // We still need a senderId to POST /friends/accept, which isn't exposed yet.
  // Workaround: use email to look the sender up via /members/search.
  const name = r.receiverName || r.senderName || r.name || "?";
  const email = r.receiverEmail || r.senderEmail || r.email || "";
  const card = el("div", { class: "friend-card" }, [
    avatar(name),
    el("div", { class: "friend-meta" }, [
      el("div", { class: "name" }, name),
      el("div", { class: "sub muted" }, email),
    ]),
    el("button", { class: "btn-primary friend-accept", onclick: () => accept(email, card, body) }, "수락"),
  ]);
  return card;
}

async function accept(email, card, body) {
  if (!email) { toast("발신자 정보를 알 수 없어요"); return; }
  try {
    const pg = await api.get(`/members/search?q=${encodeURIComponent(email)}&size=1`);
    const sender = (pg?.content || [])[0];
    if (!sender) throw new Error("발신자를 찾지 못했어요");
    await api.patch("/friends/accept", { senderId: sender.id });
    toast("친구 수락 완료");
    loadReceived(body);
  } catch (e) {
    toast(e.message || "수락 실패");
  }
}

// ---------- sent requests ----------
async function loadSent(body) {
  body.innerHTML = '<div class="muted center" style="padding:24px">불러오는 중...</div>';
  try {
    const page = await api.get("/friends/sent?page=0&size=50");
    const items = page?.content ?? [];
    body.innerHTML = "";
    if (!items.length) {
      body.appendChild(el("div", { class: "empty" }, "보낸 친구 요청이 없어요."));
      return;
    }
    items.forEach(r => {
      const name = r.receiverName || r.name || "?";
      const sub  = r.receiverEmail || r.email || "";
      body.appendChild(el("div", { class: "friend-card" }, [
        avatar(name),
        el("div", { class: "friend-meta" }, [
          el("div", { class: "name" }, name),
          el("div", { class: "sub muted" }, sub),
        ]),
        el("span", { class: "friend-pending muted" }, "대기 중"),
      ]));
    });
  } catch (e) {
    body.innerHTML = "";
    body.appendChild(el("div", { class: "empty" }, "보낸 요청을 가져오지 못했어요."));
  }
}

// ---------- search + request ----------
function loadSearch(body) {
  body.innerHTML = "";
  const input = el("input", {
    type: "search",
    class: "friend-search",
    placeholder: "이름 또는 이메일로 검색",
    autocomplete: "off",
  });
  const results = el("div", { class: "friend-results" });

  let timer = null;
  input.addEventListener("input", () => {
    clearTimeout(timer);
    const q = input.value.trim();
    if (q.length < 2) { results.innerHTML = '<div class="muted center" style="padding:20px">두 글자 이상 입력하세요.</div>'; return; }
    timer = setTimeout(() => doSearch(q, results), 250);
  });

  body.appendChild(input);
  body.appendChild(results);
  input.focus();
}

async function doSearch(q, results) {
  results.innerHTML = '<div class="muted center" style="padding:20px">검색 중...</div>';
  try {
    const page = await api.get(`/members/search?q=${encodeURIComponent(q)}&page=0&size=20`);
    const items = (page?.content || []).filter(m => Number(m.id) !== Number(auth.meId));
    results.innerHTML = "";
    if (!items.length) {
      results.appendChild(el("div", { class: "empty" }, "검색 결과가 없어요."));
      return;
    }
    items.forEach(m => results.appendChild(searchRow(m)));
  } catch (e) {
    results.innerHTML = "";
    results.appendChild(el("div", { class: "empty" }, `검색 실패: ${e.message}`));
  }
}

function searchRow(m) {
  const reqBtn = el("button", { class: "btn-primary friend-request" }, "친구 요청");
  reqBtn.addEventListener("click", async () => {
    reqBtn.disabled = true;
    try {
      await api.post("/friends", { receiverId: m.id });
      reqBtn.textContent = "요청 완료";
    } catch (e) {
      reqBtn.disabled = false;
      toast(e.message || "요청 실패");
    }
  });
  return el("div", { class: "friend-card" }, [
    avatar(m.name || `user${m.id}`),
    el("div", { class: "friend-meta" }, [
      el("div", { class: "name" }, m.name || `user${m.id}`),
      el("div", { class: "sub muted" }, m.email || ""),
    ]),
    reqBtn,
  ]);
}
