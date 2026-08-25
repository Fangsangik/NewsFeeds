import { api } from "./api.js";
import { auth, likes } from "./store.js";
import { el, avatar, toast } from "./ui.js";

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
      el("span", { class: "text" }, feed?.title ? `${feed.title} ` : ""),
      el("span", { class: "text" }, feed?.content || ""),
    ]),
  ]);
  commentsBody.insertBefore(captionRow, commentsBody.firstChild);

  const commentInput = el("input", { placeholder: "댓글 달기...", onkeydown: (e) => {
    if (e.key === "Enter") submit();
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

  async function editFeed() {
    const newTitle = prompt("제목 수정", feed?.title || "");
    if (newTitle === null) return;
    const newContent = prompt("내용 수정", feed?.content || "");
    if (newContent === null) return;
    try {
      await api.patch(`/feeds/${feedId}`, { title: newTitle, content: newContent });
      toast("수정되었습니다.");
      renderDetail(document.getElementById("app"), feedId);
    } catch (err) { toast(err.message || "수정 실패"); }
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

  const mediaImg = feed?.image
    ? el("img", { src: feed.image, alt: feed.title || "" })
    : el("div", { class: "placeholder" }, "이 게시물에는 이미지가 없습니다.");
  if (feed?.image) mediaImg.addEventListener("error", () => {
    mediaImg.replaceWith(el("div", { class: "placeholder" }, "📷"));
  });

  return el("div", { class: "detail" }, [
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
    el("span", { class: "text" }, c.content || ""),
  ]);
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
      const input = el("input", { class: "reply-input", placeholder: "답글 달기...", onkeydown: (e) => { if (e.key === "Enter") post(); } });
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
