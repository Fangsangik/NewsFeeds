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
      api.get(`/likes/${feedId}`, { auth: false }).catch(() => ({ likeCount: 0 })),
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
  const liked = likes.has(feedId);

  const likeCountEl = el("div", { class: "like-count" }, `좋아요 ${likeData?.likeCount ?? 0}개`);
  const heart = el("button", {
    class: `heart ${liked ? "on" : ""}`,
    onclick: () => onLike(feedId, heart, likeCountEl),
  }, liked ? "♥" : "♡");

  const commentsBody = el("div", { class: "side-body" });
  renderComments(commentsBody, comments);

  const captionRow = el("div", { class: "comment" }, [
    avatar(authorName),
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
      const fresh = await api.get(`/comments/feed/${feedId}`, { auth: false });
      commentsBody.querySelectorAll(".comment:not(.caption)").forEach(n => n.remove());
      renderComments(commentsBody, fresh);
    } catch (err) {
      toast(err.message || "댓글 작성 실패");
      submitBtn.disabled = false;
    }
  }
  captionRow.classList.add("caption");

  return el("div", { class: "detail" }, [
    el("div", { class: "media" },
      feed?.image
        ? el("img", { src: feed.image, alt: feed.title || "" })
        : el("div", { class: "placeholder" }, "이 게시물에는 이미지가 없습니다.")
    ),
    el("div", { class: "side" }, [
      el("div", { class: "side-head" }, [
        avatar(authorName),
        el("div", { class: "user-meta" }, [
          el("div", { class: "name" }, authorName),
          feed?.address ? el("div", { class: "sub" }, feed.address) : null,
        ]),
      ]),
      commentsBody,
      el("div", { class: "side-actions" }, [heart]),
      likeCountEl,
      el("div", { class: "composer" }, [commentInput, submitBtn]),
    ]),
  ]);
}

function renderComments(parent, comments) {
  if (!comments?.length) {
    parent.appendChild(el("div", { class: "muted center", style: { padding: "20px" } }, "첫 댓글을 남겨보세요."));
    return;
  }
  comments.forEach(c => {
    parent.appendChild(commentRow(c, false));
    (c.childComments || []).forEach(child => parent.appendChild(commentRow(child, true)));
  });
}

function commentRow(c, isChild) {
  const who = `user`;
  return el("div", { class: `comment ${isChild ? "child" : ""}` }, [
    avatar(who),
    el("div", {}, [
      el("span", { class: "name" }, who),
      el("span", { class: "text" }, c.content || ""),
    ]),
  ]);
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
    const fresh = res?.likeCount ?? await api.get(`/likes/${feedId}`, { auth: false }).then(r => r?.likeCount).catch(() => null);
    if (fresh != null) countEl.textContent = `좋아요 ${fresh}개`;
  } catch (err) {
    heartEl.classList.toggle("on", wasOn);
    heartEl.textContent = wasOn ? "♥" : "♡";
    likes.set(feedId, wasOn);
    toast(err.message || "좋아요 실패");
  }
}
