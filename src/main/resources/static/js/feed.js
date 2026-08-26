import { api } from "./api.js";
import { auth, likes } from "./store.js";
import { el, avatar, toast, escapeHtml, linkify } from "./ui.js";

let state = {
  page: 0,
  size: 10,
  loading: false,
  done: false,
  items: [],
};

let currentSort = "following"; // "following" | "latest" | "popular"

export async function renderHome(root) {
  root.innerHTML = "";
  state = { page: 0, size: 10, loading: false, done: false, items: [] };

  const mkTab = (key, label) => el("button", {
    class: `feed-tab ${currentSort === key ? "active" : ""}`,
    onclick: () => { if (currentSort !== key) { currentSort = key; renderHome(root); } },
  }, label);
  const tabs = el("div", { class: "feed-tabs" }, [
    mkTab("following", "팔로잉"),
    mkTab("latest", "최신"),
    mkTab("popular", "인기"),
  ]);
  const list = el("div", { class: "feed-list" });
  const sentinel = el("div", { class: "center muted", style: { padding: "16px" } }, "");
  root.appendChild(tabs);
  // 팔로잉 탭에서만 상단에 '알 수도 있는 사람' 가로 추천 카드
  if (currentSort === "following" && auth.isLoggedIn) {
    const rec = el("div", { class: "home-suggest" });
    root.appendChild(rec);
    renderHomeSuggestions(rec);
  }
  root.appendChild(list);
  root.appendChild(sentinel);

  async function loadMore() {
    if (state.loading || state.done) return;
    state.loading = true;
    sentinel.innerHTML = "";
    sentinel.appendChild(el("span", { class: "spinner" }));
    try {
      const endpoint = currentSort === "popular" ? "likecount"
        : currentSort === "following" ? "following" : "latest";
      // 팔로잉 피드는 로그인 필요(내 친구 기준). 나머지는 공개.
      const data = await api.get(`/feeds/${endpoint}?page=${state.page}&size=${state.size}`,
        currentSort === "following" ? {} : { auth: false });
      const items = data?.content ?? [];
      if (!items.length && state.page === 0) {
        list.appendChild(el("div", { class: "empty" }, currentSort === "following"
          ? "팔로우한 친구의 글이 없어요. 친구를 추가하거나 '최신' 탭을 눌러보세요!"
          : "아직 게시물이 없어요. 첫 글을 올려보세요!"));
      }
      items.forEach(item => list.appendChild(renderCard(item)));
      state.items.push(...items);
      state.page += 1;
      const isLast = data?.last ?? (items.length < state.size);
      if (isLast) {
        state.done = true;
        sentinel.textContent = items.length || state.items.length ? "끝이에요." : "";
        obs.disconnect(); // stop firing once we've hit the last page
      } else {
        sentinel.textContent = "";
      }
    } catch (err) {
      sentinel.textContent = `로딩 실패: ${err.message}`;
    } finally {
      state.loading = false;
    }
  }

  const obs = new IntersectionObserver((entries) => {
    entries.forEach(e => { if (e.isIntersecting) loadMore(); });
  }, { rootMargin: "200px" });
  obs.observe(sentinel);

  loadMore();
}

function renderCard(item) {
  // item: FeedWithLikeCountDto { feedId, title, content, image, likeCount }
  const feedId = item.feedId;
  const liked = likes.has(feedId);
  const likeCountEl = el("div", { class: "card-meta" }, `좋아요 ${item.likeCount}개`);
  const heart = el("button", {
    class: `heart ${liked ? "on" : ""}`,
    title: liked ? "좋아요 취소" : "좋아요",
    onclick: () => onLike(item, heart, likeCountEl),
  }, liked ? "♥" : "♡");

  const image = item.image
    ? el("img", { class: "card-image", src: item.image, alt: item.title || "" })
    : el("div", { class: "card-image placeholder" }, "📷");
  if (item.image) image.addEventListener("error", () => {
    const ph = el("div", { class: "card-image placeholder" }, "📷");
    ph.addEventListener("click", () => { location.hash = `#/feed/${feedId}`; });
    ph.style.cursor = "pointer";
    image.replaceWith(ph);
  });
  image.addEventListener("click", () => { location.hash = `#/feed/${feedId}`; });
  image.style.cursor = "pointer";

  const authorName = item.authorName || `user${item.authorId ?? ""}`;

  return el("article", { class: "card" }, [
    el("header", { class: "card-head" }, [
      avatar(authorName),
      el("div", { class: "user-meta" }, [
        el("div", { class: "name" }, authorName),
      ]),
    ]),
    image,
    el("div", { class: "card-actions" }, [heart]),
    likeCountEl,
    el("div", { class: "card-body" }, [
      item.title ? el("div", { class: "post-title" }, item.title) : null,
      item.content
        ? el("div", { class: "text" }, linkify(item.content))
        : (item.title ? null : el("span", { class: "muted" }, "—")),
    ]),
    el("a", {
      class: "card-comments-link",
      onclick: (e) => { e.preventDefault(); location.hash = `#/feed/${feedId}`; },
    }, "댓글 모두 보기"),
  ]);
}

async function onLike(item, heartEl, countEl) {
  if (!auth.isLoggedIn) {
    location.hash = "#/login";
    return;
  }
  const wasOn = heartEl.classList.contains("on");
  // Optimistic UI
  heartEl.classList.toggle("on", !wasOn);
  heartEl.textContent = wasOn ? "♡" : "♥";
  const optimistic = item.likeCount + (wasOn ? -1 : 1);
  item.likeCount = Math.max(0, optimistic);
  countEl.textContent = `좋아요 ${item.likeCount}개`;
  likes.set(item.feedId, !wasOn);

  try {
    const path = wasOn ? `/likes/dislike/${item.feedId}` : `/likes/like/${item.feedId}`;
    const res = await api.post(path);
    if (res && typeof res.likeCount === "number") {
      item.likeCount = res.likeCount;
      countEl.textContent = `좋아요 ${item.likeCount}개`;
    }
  } catch (err) {
    // Rollback on error
    heartEl.classList.toggle("on", wasOn);
    heartEl.textContent = wasOn ? "♥" : "♡";
    item.likeCount = Math.max(0, item.likeCount + (wasOn ? 1 : -1));
    countEl.textContent = `좋아요 ${item.likeCount}개`;
    likes.set(item.feedId, wasOn);
    toast(err.message || "좋아요 처리 실패");
  }
}

// ---------------- Composer modal ----------------

export function openComposer(onCreated, edit = null) {
  const backdrop = el("div", { class: "modal-backdrop", onclick: (e) => {
    if (e.target === backdrop) close();
  }});

  // items: 이미지 항목 배열. 기존 이미지는 {url}, 새로 선택한 파일은 {file}.
  let items = edit && Array.isArray(edit.images) ? edit.images.map(u => ({ url: u })) : [];

  const titleIn = el("input", { class: "title", placeholder: "제목", value: (edit && edit.title) || "" });
  const contentIn = el("textarea", { placeholder: "문구 입력..." });
  if (edit && edit.content) contentIn.value = edit.content;
  const addressIn = el("input", { placeholder: "위치 (선택)", value: (edit && edit.address) || "" });
  const errEl = el("div", { class: "error" });

  const fileIn = el("input", {
    type: "file",
    accept: "image/*",
    multiple: true,
    style: { display: "none" },
    onchange: (e) => {
      const fs = [...(e.target.files || [])];
      fs.forEach(f => items.push({ file: f }));
      e.target.value = ""; // 같은 파일 다시 선택 가능하게
      renderLeft();
    },
  });

  const left = el("div", { class: "left" });
  const right = el("div", { class: "right" }, [
    titleIn,
    contentIn,
    el("div", { class: "meta" }, addressIn),
    errEl,
  ]);

  function renderLeft() {
    left.innerHTML = "";
    if (items.length) {
      const grid = el("div", { class: "composer-thumbs" }, items.map((it, i) => {
        const src = it.url ? it.url : URL.createObjectURL(it.file);
        return el("div", { class: "composer-thumb" }, [
          el("img", { src }),
          el("button", { class: "thumb-del", title: "제거", onclick: () => { items.splice(i, 1); renderLeft(); } }, "✕"),
        ]);
      }));
      const addBtn = el("button", { class: "thumb-add", onclick: () => fileIn.click() }, "＋ 사진 추가");
      left.appendChild(grid);
      left.appendChild(addBtn);
    } else {
      const prompt = el("div", { class: "file-prompt" }, [
        el("div", { style: { fontSize: "44px" } }, "📷"),
        el("div", {}, "사진을 여러 장 선택할 수 있어요"),
        el("label", { for: "file-pick" }, "컴퓨터에서 선택"),
      ]);
      prompt.querySelector("label").addEventListener("click", () => fileIn.click());
      left.appendChild(prompt);
    }
  }
  renderLeft();

  const isEdit = !!edit;
  const shareBtn = el("button", { class: "share", onclick: share }, isEdit ? "저장" : "공유");

  function close() { backdrop.remove(); }

  async function share() {
    errEl.textContent = "";
    if (!titleIn.value.trim() && !contentIn.value.trim() && !items.length) {
      errEl.textContent = "제목이나 내용을 입력하거나 사진을 추가해주세요.";
      return;
    }
    shareBtn.disabled = true;
    shareBtn.textContent = isEdit ? "저장 중..." : "공유 중...";
    try {
      // 기존 이미지(url)는 그대로, 새 파일은 업로드 — items 순서 유지
      const images = [];
      for (const it of items) {
        if (it.url) images.push(it.url);
        else { const r = await api.uploadImage(it.file); if (r?.url) images.push(r.url); }
      }
      if (isEdit) {
        const updated = await api.patch(`/feeds/${edit.feedId}`, {
          title: titleIn.value.trim(), content: contentIn.value, images,
        });
        toast("수정되었습니다.");
        close();
        onCreated?.(updated);
      } else {
        const payload = {
          title: titleIn.value.trim(), content: contentIn.value,
          image: images[0] || null, images,
          address: addressIn.value.trim(), latitude: null, longitude: null,
        };
        const created = await api.post("/feeds", payload);
        toast("게시되었습니다.");
        close();
        onCreated?.(created);
      }
    } catch (err) {
      errEl.textContent = err.message || (isEdit ? "수정 실패" : "공유 실패");
      shareBtn.disabled = false;
      shareBtn.textContent = isEdit ? "저장" : "공유";
    }
  }

  backdrop.appendChild(
    el("div", { class: "modal" }, [
      el("div", { class: "modal-head" }, [
        el("button", { onclick: close, title: "닫기" }, "✕"),
        el("span", {}, isEdit ? "게시물 수정" : "새 게시물 만들기"),
        shareBtn,
      ]),
      el("div", { class: "modal-body" }, [left, right]),
      fileIn,
    ])
  );

  document.body.appendChild(backdrop);
}

// ---------------- 검색 (게시물/해시태그) ----------------
export async function renderSearch(root, query) {
  root.innerHTML = "";
  const input = el("input", { class: "search-input", placeholder: "게시물 검색 (예: 강남, #여행)", value: query || "" });
  const results = el("div", { class: "feed-list" });
  root.appendChild(el("div", { class: "search-bar" }, input));
  root.appendChild(results);

  let timer = null;
  async function run() {
    const q = input.value.trim();
    results.innerHTML = "";
    if (!q) { results.appendChild(el("div", { class: "empty" }, "검색어를 입력하세요.")); return; }
    results.appendChild(el("div", { class: "center muted", style: { padding: "16px" } }, el("span", { class: "spinner" })));
    try {
      const data = await api.get(`/feeds/search?q=${encodeURIComponent(q)}&page=0&size=30`, { auth: false });
      const items = data?.content ?? [];
      results.innerHTML = "";
      if (!items.length) { results.appendChild(el("div", { class: "empty" }, "검색 결과가 없어요.")); return; }
      items.forEach(item => results.appendChild(renderCard(item)));
    } catch (err) {
      results.innerHTML = "";
      results.appendChild(el("div", { class: "empty" }, `검색 실패: ${err.message}`));
    }
  }
  input.addEventListener("input", () => { clearTimeout(timer); timer = setTimeout(run, 300); });
  setTimeout(() => input.focus(), 0);
  if (query) run();
}

// ---------------- 저장한 게시물 ----------------
export async function renderSaved(root) {
  root.innerHTML = "";
  root.appendChild(el("h2", { class: "page-title" }, "저장한 게시물"));
  const list = el("div", { class: "feed-list" });
  root.appendChild(list);
  try {
    const data = await api.get(`/bookmarks?page=0&size=30`);
    const items = data?.content ?? [];
    if (!items.length) { list.appendChild(el("div", { class: "empty" }, "저장한 게시물이 없어요.")); return; }
    items.forEach(item => list.appendChild(renderCard(item)));
  } catch (err) {
    list.appendChild(el("div", { class: "empty" }, `불러오기 실패: ${err.message}`));
  }
}

// ---------------- 탐색(Explore): 인기 게시물 그리드 ----------------
export async function renderExplore(root) {
  root.innerHTML = "";
  root.appendChild(el("h2", { class: "page-title" }, "탐색"));
  const grid = el("div", { class: "explore-grid" });
  root.appendChild(grid);
  try {
    const data = await api.get("/feeds/likecount?page=0&size=30", { auth: false });
    const items = data?.content ?? [];
    if (!items.length) { grid.appendChild(el("div", { class: "empty" }, "게시물이 없어요.")); return; }
    items.forEach(item => {
      const src = item.image;
      const cell = src
        ? el("img", { class: "explore-cell", src, alt: item.title || "" })
        : el("div", { class: "explore-cell placeholder" }, "📷");
      if (src) cell.addEventListener("error", () => cell.replaceWith((() => { const p = el("div", { class: "explore-cell placeholder" }, "📷"); p.addEventListener("click", () => { location.hash = `#/feed/${item.feedId}`; }); return p; })()));
      cell.style.cursor = "pointer";
      cell.addEventListener("click", () => { location.hash = `#/feed/${item.feedId}`; });
      grid.appendChild(cell);
    });
  } catch (err) {
    grid.appendChild(el("div", { class: "empty" }, `불러오기 실패: ${err.message}`));
  }
}

// ---------------- 홈 팔로우 추천 (가로 카드) ----------------
async function renderHomeSuggestions(container) {
  try {
    const list = await api.get("/friends/suggestions?limit=8");
    if (!list?.length) { container.remove?.(); return; }
    container.appendChild(el("div", { class: "home-suggest-title" }, "알 수도 있는 사람"));
    const row = el("div", { class: "home-suggest-row" });
    list.forEach(s => {
      const card = el("div", { class: "suggest-card" }, [
        (() => { const a = avatar(s.name || `user${s.id}`, "lg", s.image); a.style.cursor = "pointer"; a.addEventListener("click", () => { location.hash = `#/profile/${s.id}`; }); return a; })(),
        el("div", { class: "suggest-name", onclick: () => { location.hash = `#/profile/${s.id}`; }, style: { cursor: "pointer" } }, s.name || `user${s.id}`),
        el("div", { class: "suggest-mutual muted" }, `공통 ${s.mutual}명`),
        el("button", { class: "btn-primary suggest-follow", onclick: async (e) => {
          const b = e.currentTarget; b.disabled = true;
          try { await api.post("/friends", { receiverId: s.id }); b.textContent = "요청됨"; }
          catch (err) { toast(err.message || "요청 실패"); b.disabled = false; }
        }}, "친구 요청"),
      ]);
      row.appendChild(card);
    });
    container.appendChild(row);
  } catch (e) { container.remove?.(); }
}
