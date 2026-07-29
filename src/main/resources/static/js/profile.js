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
      buildHeader(displayName, list.length, isMe),
      list.length === 0
        ? el("div", { class: "empty", style: { marginTop: "20px" } }, "아직 게시물이 없어요.")
        : buildGrid(list, memberId),
    ])
  );
}

function buildHeader(name, count, isMe) {
  const actions = isMe
    ? [el("button", { class: "btn-ghost", onclick: () => { location.hash = "#/"; } }, "홈으로")]
    : [];
  return el("section", { class: "profile-head" }, [
    el("div", { class: "profile-avatar" }, avatar(name, "lg")),
    el("div", { class: "profile-meta" }, [
      el("div", { class: "profile-name-row" }, [
        el("span", { class: "profile-name" }, name),
        ...actions,
      ]),
      el("div", { class: "profile-stats" }, [
        el("span", {}, [el("b", {}, String(count)), document.createTextNode(" 게시물")]),
      ]),
    ]),
  ]);
}

function buildGrid(feeds, memberId) {
  const cells = feeds.map((f, i) => {
    // Feed list endpoint returns FeedResponseDto with no id field — derive a hash route from index instead.
    const cell = el("div", { class: "grid-cell" }, [
      f.image
        ? el("img", { src: f.image, alt: f.title || "" })
        : el("div", { class: "grid-cell-placeholder" }, "📷"),
      el("div", { class: "grid-cell-overlay" }, f.title || ""),
    ]);
    // We don't have feedId here (DTO limitation); the home feed list has it.
    // For now clicking shows the title via toast; HANDOFF item 5 will expose feedId.
    cell.addEventListener("click", () => {
      toast(f.title || `게시물 #${i + 1}`);
    });
    return cell;
  });
  return el("section", { class: "profile-grid" }, cells);
}
