import { api } from "./api.js";
import { auth, likes } from "./store.js";
import { el, avatar, toast, escapeHtml } from "./ui.js";

let state = {
  page: 0,
  size: 10,
  loading: false,
  done: false,
  items: [],
};

let currentSort = "latest"; // "latest" | "popular"

export async function renderHome(root) {
  root.innerHTML = "";
  state = { page: 0, size: 10, loading: false, done: false, items: [] };

  const tabs = el("div", { class: "feed-tabs" }, [
    el("button", { class: `feed-tab ${currentSort === "latest" ? "active" : ""}`,
      onclick: () => { if (currentSort !== "latest") { currentSort = "latest"; renderHome(root); } } }, "최신"),
    el("button", { class: `feed-tab ${currentSort === "popular" ? "active" : ""}`,
      onclick: () => { if (currentSort !== "popular") { currentSort = "popular"; renderHome(root); } } }, "인기"),
  ]);
  const list = el("div", { class: "feed-list" });
  const sentinel = el("div", { class: "center muted", style: { padding: "16px" } }, "");
  root.appendChild(tabs);
  root.appendChild(list);
  root.appendChild(sentinel);

  async function loadMore() {
    if (state.loading || state.done) return;
    state.loading = true;
    sentinel.innerHTML = "";
    sentinel.appendChild(el("span", { class: "spinner" }));
    try {
      const endpoint = currentSort === "popular" ? "likecount" : "latest";
      const data = await api.get(`/feeds/${endpoint}?page=${state.page}&size=${state.size}`, { auth: false });
      const items = data?.content ?? [];
      if (!items.length && state.page === 0) {
        list.appendChild(el("div", { class: "empty" }, "아직 게시물이 없어요. 첫 글을 올려보세요!"));
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
      item.title ? el("span", { class: "title" }, `${item.title} `) : null,
      item.content
        ? el("span", { class: "text" }, item.content)
        : el("span", { class: "muted" }, item.title ? "" : "—"),
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

export function openComposer(onCreated) {
  const backdrop = el("div", { class: "modal-backdrop", onclick: (e) => {
    if (e.target === backdrop) close();
  }});

  let pickedFile = null;
  let pickedUrl = null;
  let uploadedUrl = null;

  const titleIn = el("input", { class: "title", placeholder: "제목" });
  const contentIn = el("textarea", { placeholder: "문구 입력..." });
  const addressIn = el("input", { placeholder: "위치 (선택)" });
  const errEl = el("div", { class: "error" });

  const fileIn = el("input", {
    type: "file",
    accept: "image/*",
    style: { display: "none" },
    onchange: async (e) => {
      const f = e.target.files?.[0];
      if (!f) return;
      pickedFile = f;
      pickedUrl = URL.createObjectURL(f);
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
    if (pickedUrl) {
      left.appendChild(el("img", { src: pickedUrl }));
    } else {
      const prompt = el("div", { class: "file-prompt" }, [
        el("div", { style: { fontSize: "44px" } }, "📷"),
        el("div", {}, "사진을 여기에 끌어다 놓으세요"),
        el("label", { for: "file-pick" }, "컴퓨터에서 선택"),
      ]);
      const label = prompt.querySelector("label");
      label.addEventListener("click", () => fileIn.click());
      left.appendChild(prompt);
    }
  }
  renderLeft();

  const shareBtn = el("button", { class: "share", onclick: share }, "공유");

  function close() { backdrop.remove(); }

  async function share() {
    errEl.textContent = "";
    // 사진은 선택 항목. 제목이나 내용 중 하나만 있으면 글만으로도 게시 가능.
    if (!titleIn.value.trim() && !contentIn.value.trim()) {
      errEl.textContent = "제목이나 내용을 입력해주세요.";
      return;
    }
    shareBtn.disabled = true;
    shareBtn.textContent = "공유 중...";
    try {
      if (pickedFile && !uploadedUrl) {
        const r = await api.uploadImage(pickedFile);
        uploadedUrl = r?.url;
        if (!uploadedUrl) throw new Error("이미지 업로드 실패");
      }
      const payload = {
        title: titleIn.value.trim(),
        content: contentIn.value,
        image: uploadedUrl,
        address: addressIn.value.trim(),
        latitude: null,
        longitude: null,
      };
      const created = await api.post("/feeds", payload);
      toast("게시되었습니다.");
      close();
      onCreated?.(created);
    } catch (err) {
      errEl.textContent = err.message || "공유 실패";
      shareBtn.disabled = false;
      shareBtn.textContent = "공유";
    }
  }

  backdrop.appendChild(
    el("div", { class: "modal" }, [
      el("div", { class: "modal-head" }, [
        el("button", { onclick: close, title: "닫기" }, "✕"),
        el("span", {}, "새 게시물 만들기"),
        shareBtn,
      ]),
      el("div", { class: "modal-body" }, [left, right]),
      fileIn,
    ])
  );

  document.body.appendChild(backdrop);
}
