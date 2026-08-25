import { api } from "./api.js";
import { el, fmtTimeAgo } from "./ui.js";

const ICON = { LIKE: "❤️", COMMENT: "💬", FRIEND_REQUEST: "👥", FRIEND_ACCEPT: "✅" };

export async function renderNotifications(root) {
  root.innerHTML = "";
  root.appendChild(el("h2", { class: "page-title" }, "알림"));
  const list = el("div", { class: "noti-list" });
  root.appendChild(list);

  let items = [];
  try {
    const data = await api.get("/notifications?page=0&size=50");
    items = data?.content ?? [];
  } catch (err) {
    list.appendChild(el("div", { class: "empty" }, `불러오기 실패: ${err.message}`));
    return;
  }

  if (!items.length) {
    list.appendChild(el("div", { class: "empty" }, "알림이 없어요."));
  } else {
    items.forEach(n => {
      const row = el("button", { class: `noti-row ${n.readStatus ? "" : "unread"}`, onclick: () => {
        if (n.feedId) location.hash = `#/feed/${n.feedId}`;
        else location.hash = "#/friends";
      }}, [
        el("span", { class: "noti-icon" }, ICON[n.type] || "🔔"),
        el("div", { class: "noti-body" }, [
          el("div", { class: "noti-msg" }, n.message || ""),
          el("div", { class: "noti-time muted" }, fmtTimeAgo(n.createdAt) || ""),
        ]),
      ]);
      list.appendChild(row);
    });
  }

  // 목록을 열면 모두 읽음 처리 (뱃지 정리)
  api.patch("/notifications/read-all").catch(() => {});
}
