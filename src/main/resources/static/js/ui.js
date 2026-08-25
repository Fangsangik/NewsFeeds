// Tiny DOM helpers shared across modules. No framework, no dependencies.

export function el(tag, attrs = {}, children = []) {
  const node = document.createElement(tag);
  Object.entries(attrs || {}).forEach(([k, v]) => {
    if (v == null || v === false) return;
    if (k === "class") node.className = v;
    else if (k === "html") node.innerHTML = v;
    else if (k === "style" && typeof v === "object") Object.assign(node.style, v);
    else if (k.startsWith("on") && typeof v === "function") node.addEventListener(k.slice(2), v);
    else node.setAttribute(k, v === true ? "" : v);
  });
  (Array.isArray(children) ? children : [children]).forEach(c => {
    if (c == null || c === false) return;
    node.appendChild(typeof c === "string" || typeof c === "number"
      ? document.createTextNode(String(c))
      : c);
  });
  return node;
}

export function avatar(seed, size = "sm", imageUrl = null) {
  const cls = `avatar ${size === "lg" ? "lg" : ""}`;
  if (imageUrl) {
    return el("div", { class: cls }, el("img", { src: imageUrl, alt: seed || "" }));
  }
  const initial = (seed || "?").trim().charAt(0).toUpperCase();
  return el("div", { class: cls }, el("div", {}, initial));
}

export function toast(message, timeoutMs = 2200) {
  const node = el("div", { class: "toast" }, message);
  document.body.appendChild(node);
  setTimeout(() => node.remove(), timeoutMs);
}

export function fmtTimeAgo(iso) {
  if (!iso) return "";
  const date = new Date(iso);
  const diff = (Date.now() - date.getTime()) / 1000;
  if (diff < 60) return "방금 전";
  if (diff < 3600) return `${Math.floor(diff / 60)}분 전`;
  if (diff < 86400) return `${Math.floor(diff / 3600)}시간 전`;
  if (diff < 86400 * 7) return `${Math.floor(diff / 86400)}일 전`;
  return date.toLocaleDateString();
}

export function escapeHtml(s) {
  if (s == null) return "";
  return String(s).replace(/[&<>"']/g, c => ({
    "&": "&amp;", "<": "&lt;", ">": "&gt;", '"': "&quot;", "'": "&#39;",
  }[c]));
}
