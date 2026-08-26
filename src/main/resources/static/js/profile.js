import { api } from "./api.js";
import { auth } from "./store.js";
import { el, avatar, toast } from "./ui.js";

export async function renderProfile(root, memberId) {
  root.innerHTML = "";
  root.appendChild(el("div", { class: "center muted", style: { padding: "40px" } }, [
    el("span", { class: "spinner" }),
  ]));

  let member, feeds;
  try {
    [member, feeds] = await Promise.all([
      api.get(`/members/${memberId}`, { auth: false }).catch(() => null),
      api.get(`/feeds/members/${memberId}`, { auth: false }).catch(() => []),
    ]);
  } catch (err) {
    root.innerHTML = "";
    root.appendChild(el("div", { class: "empty" }, `프로필 로딩 실패: ${err.message}`));
    return;
  }

  const list = Array.isArray(feeds) ? feeds : [];
  const isMe = auth.meId && Number(auth.meId) === Number(memberId);
  // The /members/{id} response only carries the id; if it's me, fall back to localStorage.
  const displayName = (isMe && auth.meEmail ? auth.meEmail.split("@")[0] : null)
    || (member && member.name)
    || `user${memberId}`;

  root.innerHTML = "";
  root.appendChild(
    el("div", {}, [
      buildHeader(displayName, list.length, isMe, member && member.image, memberId, member),
      list.length === 0
        ? el("div", { class: "empty", style: { marginTop: "20px" } }, "아직 게시물이 없어요.")
        : buildGrid(list, memberId),
    ])
  );
}

function buildHeader(name, count, isMe, image, memberId, member) {
  let actions;
  if (isMe) {
    actions = [
      el("button", { class: "btn-ghost", onclick: () => openProfileEditor(memberId, member) }, "프로필 편집"),
      el("button", { class: "btn-ghost", onclick: () => { location.hash = "#/"; } }, "홈으로"),
    ];
  } else {
    // 타인 프로필: 관계 상태에 따라 팔로우(친구요청)/친구끊기 + 메시지
    const box = el("div", { class: "profile-actions" });
    actions = [box];
    const reload = () => renderProfile(document.getElementById("app"), memberId);
    box.appendChild(el("button", { class: "btn-ghost", onclick: () => { location.hash = `#/dm/${memberId}`; } }, "메시지"));
    const rel = el("button", { class: "btn-ghost", disabled: true }, "…");
    box.appendChild(rel);
    api.get(`/friends/status/${memberId}`).then(s => {
      const st = s?.status;
      rel.disabled = false;
      if (st === "friends") { rel.textContent = "친구 끊기"; rel.onclick = async () => { try { await api.del(`/friends/by-member/${memberId}`); toast("친구를 끊었어요."); reload(); } catch (e) { toast(e.message || "실패"); } }; }
      else if (st === "requested_by_me") { rel.textContent = "요청됨"; rel.disabled = true; }
      else if (st === "requested_to_me") { rel.textContent = "요청 수락"; rel.classList.add("accent"); rel.onclick = async () => { try { await api.patch("/friends/accept", { senderId: memberId }); toast("친구가 되었어요."); reload(); } catch (e) { toast(e.message || "실패"); } }; }
      else { rel.textContent = "친구 요청"; rel.classList.add("accent"); rel.onclick = async () => { try { await api.post("/friends", { receiverId: memberId }); toast("친구 요청을 보냈어요."); reload(); } catch (e) { toast(e.message || "실패"); } }; }
    }).catch(() => { rel.textContent = "친구 요청"; rel.disabled = false; });
  }
  return el("section", { class: "profile-head" }, [
    el("div", { class: "profile-avatar" }, avatar(name, "lg", image)),
    el("div", { class: "profile-meta" }, [
      el("div", { class: "profile-name-row" }, [
        el("span", { class: "profile-name" }, name),
        ...actions,
      ]),
      (() => {
        // 상호 친구 모델이라 팔로워 = 팔로잉 = 친구 수. 클릭 시 친구 목록 모달.
        const followerB = el("b", {}, "0");
        const followingB = el("b", {}, "0");
        api.get(`/friends/count/${memberId}`).then(r => {
          const n = String(r?.friends ?? 0);
          followerB.textContent = n; followingB.textContent = n;
        }).catch(() => {});
        return el("div", { class: "profile-stats" }, [
          el("span", {}, [el("b", {}, String(count)), document.createTextNode(" 게시물")]),
          el("span", { class: "stat-link", onclick: () => openFriendList(memberId, "팔로워") }, [followerB, document.createTextNode(" 팔로워")]),
          el("span", { class: "stat-link", onclick: () => openFriendList(memberId, "팔로잉") }, [followingB, document.createTextNode(" 팔로잉")]),
        ]);
      })(),
    ]),
  ]);
}

// 팔로워/팔로잉(친구) 목록 모달
async function openFriendList(memberId, title) {
  const listEl = el("div", { class: "friend-modal-list" }, el("div", { class: "muted center", style: { padding: "20px" } }, "불러오는 중..."));
  const backdrop = el("div", { class: "modal-backdrop", onclick: (e) => { if (e.target === backdrop) backdrop.remove(); } });
  backdrop.appendChild(el("div", { class: "modal small" }, [
    el("div", { class: "modal-head" }, [
      el("button", { onclick: () => backdrop.remove(), title: "닫기" }, "✕"),
      el("span", {}, title),
      el("span", {}, ""),
    ]),
    listEl,
  ]));
  document.body.appendChild(backdrop);
  try {
    const members = await api.get(`/friends/members/${memberId}`);
    listEl.innerHTML = "";
    if (!members?.length) { listEl.appendChild(el("div", { class: "empty" }, "아직 없어요.")); return; }
    members.forEach(m => {
      const row = el("button", { class: "friend-modal-row", onclick: () => { backdrop.remove(); location.hash = `#/profile/${m.id}`; } }, [
        avatar(m.name || `user${m.id}`, "sm", m.image),
        el("div", { class: "name" }, m.name || `user${m.id}`),
      ]);
      listEl.appendChild(row);
    });
  } catch (e) {
    listEl.innerHTML = "";
    listEl.appendChild(el("div", { class: "empty" }, "목록을 가져오지 못했어요."));
  }
}

function buildGrid(feeds, memberId) {
  const cells = feeds.map((f, i) => {
    const img = f.image
      ? el("img", { src: f.image, alt: f.title || "" })
      : el("div", { class: "grid-cell-placeholder" }, "📷");
    if (f.image) img.addEventListener("error", () => img.replaceWith(el("div", { class: "grid-cell-placeholder" }, "📷")));
    const cell = el("div", { class: "grid-cell" }, [
      img,
      el("div", { class: "grid-cell-overlay" }, f.title || ""),
    ]);
    // FeedResponseDto가 feedId를 포함하므로 클릭 시 상세로 이동한다.
    cell.style.cursor = "pointer";
    cell.addEventListener("click", () => {
      if (f.feedId != null) location.hash = `#/feed/${f.feedId}`;
      else toast(f.title || `게시물 #${i + 1}`);
    });
    return cell;
  });
  return el("section", { class: "profile-grid" }, cells);
}

// ---------- 프로필 편집 모달 ----------
function openProfileEditor(memberId, member) {
  let pickedFile = null;
  const nameIn = el("input", { class: "title", placeholder: "이름", value: (member && member.name) || "" });
  const phoneIn = el("input", { placeholder: "휴대폰 번호 (예: 010-1234-5678)", value: (member && member.phoneNumber) || "" });
  const addrIn = el("input", { placeholder: "주소", value: (member && member.address) || "" });
  const pwIn = el("input", { type: "password", placeholder: "현재 비밀번호 (변경 확인용, 필수)" });
  const errEl = el("div", { class: "error" });

  const preview = el("div", { class: "profile-avatar" }, avatar((member && member.name) || "?", "lg", member && member.image));
  const fileIn = el("input", { type: "file", accept: "image/*", style: { display: "none" }, onchange: (e) => {
    const f = e.target.files?.[0]; if (!f) return;
    pickedFile = f;
    preview.innerHTML = "";
    preview.appendChild(avatar("", "lg", URL.createObjectURL(f)));
  }});
  const pickBtn = el("button", { class: "btn-ghost", onclick: () => fileIn.click() }, "사진 변경");

  const saveBtn = el("button", { class: "share", onclick: save }, "저장");
  function close() { backdrop.remove(); }

  async function save() {
    errEl.textContent = "";
    if (!pwIn.value) { errEl.textContent = "현재 비밀번호를 입력해주세요."; return; }
    saveBtn.disabled = true; saveBtn.textContent = "저장 중...";
    try {
      let imageUrl = member && member.image;
      if (pickedFile) { const r = await api.uploadImage(pickedFile); imageUrl = r?.url || imageUrl; }
      await api.put("/members/update", {
        id: memberId,
        password: pwIn.value,
        name: nameIn.value.trim(),
        phoneNumber: phoneIn.value.trim(),
        address: addrIn.value.trim(),
        image: imageUrl || "",
      });
      toast("프로필이 수정되었습니다.");
      close();
      renderProfile(document.getElementById("app"), memberId);
    } catch (err) {
      errEl.textContent = err.message || "수정 실패";
      saveBtn.disabled = false; saveBtn.textContent = "저장";
    }
  }

  const backdrop = el("div", { class: "modal-backdrop", onclick: (e) => { if (e.target === backdrop) close(); } });
  backdrop.appendChild(
    el("div", { class: "modal small" }, [
      el("div", { class: "modal-head" }, [
        el("button", { onclick: close, title: "닫기" }, "✕"),
        el("span", {}, "프로필 편집"),
        saveBtn,
      ]),
      el("div", { class: "modal-body col" }, [
        el("div", { class: "profile-edit-avatar" }, [preview, pickBtn]),
        nameIn, phoneIn, addrIn, pwIn, errEl, fileIn,
      ]),
    ])
  );
  document.body.appendChild(backdrop);
}
