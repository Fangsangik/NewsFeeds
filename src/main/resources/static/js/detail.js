import { api } from "./api.js";
import { auth, likes } from "./store.js";
import { el, avatar, toast, linkify } from "./ui.js";
import { openComposer } from "./feed.js";

export async function renderDetail(root, feedId) {
  root.innerHTML = "";
  root.appendChild(el("div", { class: "center muted", style: { padding: "40px" } }, [
    el("span", { class: "spinner" }),
  ]));

  let feed, likeData, comments;
  try {
    // 피드 응답(FeedResponseDto)에 author가 포함되므로 작성자 정보를 위한
    // 별도 /members/{feedId}/member 호출이 필요 없다. 나머지만 병렬로 가져온다.
    [feed, likeData, comments] = await Promise.all([
      api.get(`/feeds/${feedId}`, { auth: false }),
      // 토큰을 함께 보내 서버가 likedByMe(내가 눌렀는지)를 정확히 반환하게 한다.
      api.get(`/likes/${feedId}`).catch(() => ({ likeCount: 0, likedByMe: false })),
      api.get(`/comments/feed/${feedId}`, { auth: false }).catch(() => []),
    ]);
  } catch (err) {
    root.innerHTML = "";
    root.appendChild(el("div", { class: "empty" }, `불러오기 실패: ${err.message}`));
    return;
  }

  root.innerHTML = "";
  root.appendChild(buildView(feedId, feed, likeData, comments));
}

function buildView(feedId, feed, likeData, comments) {
  const author = feed?.author;
  const authorName = author?.name || author?.email?.split("@")[0] || `user${author?.id ?? ""}`;
  // 서버의 likedByMe가 있으면 그것을 사용(정합성), 없으면 로컬 캐시로 폴백.
  const liked = likeData?.likedByMe ?? likes.has(feedId);
  likes.set(feedId, liked);
  const isMine = auth.meId && author?.id && Number(auth.meId) === Number(author.id);

  const likeCountEl = el("div", { class: "like-count" }, `좋아요 ${likeData?.likeCount ?? 0}개`);
  const heart = el("button", {
    class: `heart ${liked ? "on" : ""}`,
    onclick: () => onLike(feedId, heart, likeCountEl),
  }, liked ? "♥" : "♡");

  const commentsBody = el("div", { class: "side-body" });

  async function refresh() {
    const fresh = await api.get(`/comments/feed/${feedId}`, { auth: false });
    commentsBody.querySelectorAll(".comment:not(.caption)").forEach(n => n.remove());
    renderComments(commentsBody, fresh, feedId, refresh);
  }

  renderComments(commentsBody, comments, feedId, refresh);

  const captionRow = el("div", { class: "comment" }, [
    avatar(authorName, "sm", author && author.image),
    el("div", {}, [
      el("span", { class: "name" }, authorName),
      feed?.title ? el("div", { class: "post-title" }, feed.title) : null,
      feed?.content ? el("div", { class: "text" }, linkify(feed.content)) : null,
    ]),
  ]);
  commentsBody.insertBefore(captionRow, commentsBody.firstChild);

  const commentInput = el("input", { placeholder: "댓글 달기...", onkeydown: (e) => {
    if (e.key === "Enter" && !e.isComposing) submit();
  }});
  const submitBtn = el("button", { disabled: true, onclick: submit }, "게시");
  commentInput.addEventListener("input", () => {
    submitBtn.disabled = commentInput.value.trim().length === 0;
  });

  async function submit() {
    if (!auth.isLoggedIn) { location.hash = "#/login"; return; }
    const text = commentInput.value.trim();
    if (!text) return;
    submitBtn.disabled = true;
    try {
      await api.post("/comments", {
        memberId: auth.meId,
        feedId,
        parentId: null,
        content: text,
      });
      commentInput.value = "";
      await refresh();
    } catch (err) {
      toast(err.message || "댓글 작성 실패");
      submitBtn.disabled = false;
    }
  }
  captionRow.classList.add("caption");

  function editFeed() {
    const imgs = (feed?.images && feed.images.length) ? feed.images : (feed?.image ? [feed.image] : []);
    openComposer(() => renderDetail(document.getElementById("app"), feedId), {
      feedId, title: feed?.title, content: feed?.content, address: feed?.address, images: imgs,
    });
  }

  async function deleteFeed() {
    if (!confirm("이 게시물을 삭제할까요?")) return;
    try {
      await api.del(`/feeds/${feedId}`);
      toast("삭제되었습니다.");
      location.hash = "#/";
    } catch (err) { toast(err.message || "삭제 실패"); }
  }

  const ownerActions = isMine
    ? el("div", { class: "owner-actions" }, [
        el("button", { class: "owner-btn", onclick: editFeed }, "수정"),
        el("button", { class: "owner-btn danger", onclick: deleteFeed }, "삭제"),
      ])
    : null;

  // 저장(북마크) + 공유(링크 복사)
  const bookmarkBtn = el("button", { class: "iconbtn bookmark", title: "저장", onclick: toggleBookmark }, "🔖");
  api.get(`/bookmarks/${feedId}`).then(r => { if (r?.bookmarked) bookmarkBtn.classList.add("on"); }).catch(() => {});
  async function toggleBookmark() {
    if (!auth.isLoggedIn) { location.hash = "#/login"; return; }
    try {
      const r = await api.post(`/bookmarks/${feedId}`);
      bookmarkBtn.classList.toggle("on", !!r?.bookmarked);
      toast(r?.bookmarked ? "저장했어요." : "저장을 해제했어요.");
    } catch (err) { toast(err.message || "저장 실패"); }
  }
  const shareBtn = el("button", { class: "iconbtn share-link", title: "링크 복사", onclick: shareLink }, "🔗");
  async function shareLink() {
    const url = `${location.origin}/#/feed/${feedId}`;
    try { await navigator.clipboard.writeText(url); toast("링크가 복사되었습니다."); }
    catch { toast(url); }
  }

  // 다중 이미지 캐러셀 (images 우선, 없으면 단일 image, 없으면 placeholder)
  const imgs = (feed?.images && feed.images.length) ? feed.images : (feed?.image ? [feed.image] : []);
  const mediaImg = buildMedia(imgs, feed?.title || "");

  const closeBtn = el("button", {
    class: "detail-close", title: "닫기",
    onclick: () => { if (window.history.length > 1) window.history.back(); else location.hash = "#/"; },
  }, "✕");

  return el("div", { class: "detail" }, [
    closeBtn,
    el("div", { class: "media" }, mediaImg),
    el("div", { class: "side" }, [
      el("div", { class: "side-head" }, [
        avatar(authorName, "sm", author && author.image),
        el("div", { class: "user-meta" }, [
          el("div", { class: "name" }, authorName),
          feed?.address ? el("div", { class: "sub" }, feed.address) : null,
        ]),
        ownerActions,
      ]),
      commentsBody,
      el("div", { class: "side-actions" }, [heart, bookmarkBtn, shareBtn]),
      likeCountEl,
      el("div", { class: "composer" }, [commentInput, submitBtn]),
    ]),
  ]);
}

function renderComments(parent, comments, feedId, refresh) {
  if (!comments?.length) {
    parent.appendChild(el("div", { class: "muted center", style: { padding: "20px" } }, "첫 댓글을 남겨보세요."));
    return;
  }
  comments.forEach(c => {
    parent.appendChild(commentRow(c, false, feedId, refresh));
    (c.childComments || []).forEach(child => parent.appendChild(commentRow(child, true, feedId, refresh)));
  });
}

function commentRow(c, isChild, feedId, refresh) {
  const who = c.authorName || `user${c.authorId ?? ""}`;
  const meta = el("div", {}, [
    el("span", { class: "name" }, who),
    el("span", { class: "text" }, linkify(c.content || "")),
  ]);

  // 댓글 좋아요(♥) — 낙관적 토글 + 서버 응답으로 확정
  let liked = !!c.likedByMe;
  let cnt = Number(c.likeCount || 0);
  const cLikeCount = el("span", { class: "clike-count" }, cnt ? String(cnt) : "");
  const cHeart = el("button", { class: `clike ${liked ? "on" : ""}`, title: "좋아요", onclick: async () => {
    if (!auth.isLoggedIn) { location.hash = "#/login"; return; }
    liked = !liked; cnt = Math.max(0, cnt + (liked ? 1 : -1));
    cHeart.classList.toggle("on", liked); cHeart.textContent = liked ? "♥" : "♡";
    cLikeCount.textContent = cnt ? String(cnt) : "";
    try {
      const r = await api.post(`/comment-likes/${c.commentId}`);
      if (r && typeof r.liked === "boolean") {
        liked = r.liked; cnt = Number(r.count || 0);
        cHeart.classList.toggle("on", liked); cHeart.textContent = liked ? "♥" : "♡";
        cLikeCount.textContent = cnt ? String(cnt) : "";
      }
    } catch (err) { toast(err.message || "좋아요 실패"); }
  }}, liked ? "♥" : "♡");
  meta.appendChild(el("div", { class: "clike-row" }, [cHeart, cLikeCount]));

  const row = el("div", { class: `comment ${isChild ? "child" : ""}` }, [avatar(who), meta]);

  // 내 댓글이면 삭제 버튼
  if (auth.meId && c.authorId && Number(auth.meId) === Number(c.authorId) && refresh) {
    const del = el("button", { class: "comment-del", title: "삭제", onclick: async () => {
      if (!confirm("댓글을 삭제할까요?")) return;
      try { await api.del(`/comments/${c.commentId}`); await refresh(); }
      catch (err) { toast(err.message || "삭제 실패"); }
    }}, "×");
    row.appendChild(del);
  }

  // 답글은 최상위 댓글에만 (백엔드는 1단계 대댓글 지원)
  if (!isChild && feedId && refresh) {
    let box = null;
    const replyLink = el("a", { class: "reply-link", onclick: (e) => { e.preventDefault(); toggle(); } }, "답글 달기");
    meta.appendChild(el("div", {}, replyLink));

    function toggle() {
      if (box) { box.remove(); box = null; return; }
      const input = el("input", { class: "reply-input", placeholder: "답글 달기...", onkeydown: (e) => { if (e.key === "Enter" && !e.isComposing) post(); } });
      const btn = el("button", { class: "reply-send", onclick: post }, "게시");
      box = el("div", { class: "reply-box" }, [input, btn]);
      meta.appendChild(box);
      input.focus();

      async function post() {
        if (!auth.isLoggedIn) { location.hash = "#/login"; return; }
        const text = input.value.trim();
        if (!text) return;
        btn.disabled = true;
        try {
          await api.post("/comments", { memberId: auth.meId, feedId, parentId: c.commentId, content: text });
          await refresh();
        } catch (err) {
          toast(err.message || "답글 작성 실패");
          btn.disabled = false;
        }
      }
    }
  }
  return row;
}

async function onLike(feedId, heartEl, countEl) {
  if (!auth.isLoggedIn) { location.hash = "#/login"; return; }
  const wasOn = heartEl.classList.contains("on");
  heartEl.classList.toggle("on", !wasOn);
  heartEl.textContent = wasOn ? "♡" : "♥";
  likes.set(feedId, !wasOn);
  try {
    const path = wasOn ? `/likes/dislike/${feedId}` : `/likes/like/${feedId}`;
    const res = await api.post(path);
    // 서버 응답(likeCount/likedByMe)을 신뢰해 최종 상태를 확정한다.
    if (res && typeof res.likedByMe === "boolean") {
      heartEl.classList.toggle("on", res.likedByMe);
      heartEl.textContent = res.likedByMe ? "♥" : "♡";
      likes.set(feedId, res.likedByMe);
    }
    if (res && res.likeCount != null) countEl.textContent = `좋아요 ${res.likeCount}개`;
  } catch (err) {
    heartEl.classList.toggle("on", wasOn);
    heartEl.textContent = wasOn ? "♥" : "♡";
    likes.set(feedId, wasOn);
    toast(err.message || "좋아요 실패");
  }
}

// 다중 이미지 캐러셀 (1장이면 단순 이미지, 0장이면 placeholder)
function buildMedia(imgs, alt) {
  if (!imgs.length) return el("div", { class: "placeholder" }, "이 게시물에는 이미지가 없습니다.");
  if (imgs.length === 1) {
    const img = el("img", { src: imgs[0], alt });
    img.addEventListener("error", () => img.replaceWith(el("div", { class: "placeholder" }, "📷")));
    return img;
  }
  let idx = 0;
  const imgEl = el("img", { src: imgs[0], alt });
  imgEl.addEventListener("error", () => { imgEl.style.opacity = "0.3"; });
  const dots = el("div", { class: "carousel-dots" }, imgs.map((_, i) => el("span", { class: `dot ${i === 0 ? "on" : ""}` })));
  const counter = el("div", { class: "carousel-count" }, `1/${imgs.length}`);
  function show(i) {
    idx = (i + imgs.length) % imgs.length;
    imgEl.style.opacity = "1";
    imgEl.src = imgs[idx];
    [...dots.children].forEach((d, k) => d.classList.toggle("on", k === idx));
    counter.textContent = `${idx + 1}/${imgs.length}`;
  }
  const prev = el("button", { class: "carousel-nav prev", onclick: () => show(idx - 1) }, "‹");
  const next = el("button", { class: "carousel-nav next", onclick: () => show(idx + 1) }, "›");
  const wrap = el("div", { class: "carousel" }, [imgEl, prev, next, dots, counter]);

  // 터치/마우스 스와이프: 가로 이동이 40px 넘으면 이전/다음.
  let startX = null;
  const onStart = (x) => { startX = x; };
  const onEnd = (x) => {
    if (startX == null) return;
    const dx = x - startX; startX = null;
    if (Math.abs(dx) > 40) show(dx < 0 ? idx + 1 : idx - 1);
  };
  wrap.addEventListener("touchstart", (e) => onStart(e.touches[0].clientX), { passive: true });
  wrap.addEventListener("touchend", (e) => onEnd(e.changedTouches[0].clientX), { passive: true });
  wrap.addEventListener("mousedown", (e) => onStart(e.clientX));
  wrap.addEventListener("mouseup", (e) => onEnd(e.clientX));
  imgEl.addEventListener("dragstart", (e) => e.preventDefault()); // 이미지 드래그 고스트 방지
  return wrap;
}
