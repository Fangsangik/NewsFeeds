import { auth } from "./store.js";
import { renderLogin, renderSignup, logout } from "./auth.js";
import { renderHome, openComposer, renderSearch, renderSaved } from "./feed.js";
import { renderDetail } from "./detail.js";
import { renderProfile } from "./profile.js";
import { renderDm, connectStomp, disconnectStomp } from "./dm.js";
import { renderFriends } from "./friends.js";
import { el } from "./ui.js";
import { api } from "./api.js";

const root = document.getElementById("app");

async function updateUnreadBadge(badge) {
  if (!auth.isLoggedIn) return;
  try {
    const data = await api.get("/messages/unread?page=0&size=1");
    const n = data?.totalElements ?? 0;
    badge.textContent = n > 99 ? "99+" : String(n);
    badge.style.display = n > 0 ? "flex" : "none";
  } catch { /* 뱃지는 실패해도 조용히 무시 */ }
}

function buildTopbar() {
  const topbar = document.getElementById("topbar");
  topbar.innerHTML = "";
  topbar.appendChild(
    el("div", { class: "inner" }, [
      el("div", { class: "brand", onclick: () => { location.hash = "#/"; } }, "Newsfeed"),
      el("div", { class: "nav-actions" }, auth.isLoggedIn ? [
        el("button", { class: "icon-btn", title: "새 게시물", onclick: () => openComposer(() => route()) }, "＋"),
        el("button", { class: "icon-btn", title: "홈", onclick: () => { location.hash = "#/"; } }, "🏠"),
        el("button", { class: "icon-btn", title: "검색", onclick: () => { location.hash = "#/search"; } }, "🔍"),
        el("button", { class: "icon-btn", title: "저장한 게시물", onclick: () => { location.hash = "#/saved"; } }, "🔖"),
        (() => {
          const badge = el("span", { class: "nav-badge", style: { display: "none" } }, "");
          const btn = el("button", { class: "icon-btn badge-wrap", title: "메시지", onclick: () => { location.hash = "#/dm"; } }, ["💬", badge]);
          updateUnreadBadge(badge);
          return btn;
        })(),
        el("button", { class: "icon-btn", title: "친구", onclick: () => { location.hash = "#/friends"; } }, "👥"),
        el("button", { class: "icon-btn", title: "내 프로필", onclick: () => {
          if (auth.meId) location.hash = `#/profile/${auth.meId}`;
        }}, "👤"),
        el("button", { class: "icon-btn", title: "로그아웃", onclick: () => { disconnectStomp(); logout(); } }, "⎋"),
      ] : [
        el("a", { class: "muted-link", href: "#/login" }, "로그인"),
      ]),
    ])
  );
}

function route() {
  buildTopbar();
  const hash = location.hash || "#/";

  const isAuthPage = hash === "#/login" || hash === "#/signup";
  if (!auth.isLoggedIn && !isAuthPage) {
    location.hash = "#/login";
    return;
  }
  if (auth.isLoggedIn && isAuthPage) {
    location.hash = "#/";
    return;
  }

  if (hash === "#/login") return renderLogin(root);
  if (hash === "#/signup") return renderSignup(root);

  // Maintain a single live STOMP connection while the user is logged in.
  if (auth.isLoggedIn) connectStomp();

  const detailMatch = hash.match(/^#\/feed\/(\d+)/);
  if (detailMatch) return renderDetail(root, Number(detailMatch[1]));

  const profileMatch = hash.match(/^#\/profile\/(\d+)/);
  if (profileMatch) return renderProfile(root, Number(profileMatch[1]));

  const dmMatch = hash.match(/^#\/dm(?:\/(\d+))?/);
  if (dmMatch) return renderDm(root, dmMatch[1]);

  if (hash === "#/friends" || hash.startsWith("#/friends/")) return renderFriends(root);

  if (hash === "#/saved") return renderSaved(root);

  const searchMatch = hash.match(/^#\/search(?:\/(.*))?/);
  if (searchMatch) return renderSearch(root, searchMatch[1] ? decodeURIComponent(searchMatch[1]) : "");

  return renderHome(root);
}

window.addEventListener("hashchange", route);
window.addEventListener("DOMContentLoaded", route);
