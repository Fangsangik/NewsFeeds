import { auth } from "./store.js";

const BASE = ""; // same-origin

class ApiError extends Error {
  constructor(message, status, body) {
    super(message);
    this.status = status;
    this.body = body;
    // {fieldName: "에러 메시지"} when the server returned per-field validation errors
    this.fieldErrors = (body && typeof body === "object" && body.fieldErrors) || null;
  }
}

async function tryRefresh() {
  const refreshToken = auth.refreshToken;
  if (!refreshToken) return false;
  try {
    const res = await fetch(`${BASE}/auth/refresh`, {
      method: "POST",
      headers: { "Content-Type": "application/json" },
      body: JSON.stringify({ refreshToken }),
    });
    if (!res.ok) return false;
    const body = await res.json();
    const newAccess = body?.accessToken;
    if (!newAccess) return false;
    auth.setAccessToken(newAccess);
    return true;
  } catch {
    return false;
  }
}

// access token의 exp를 디코드해 만료 여부 판단 (선제 갱신용)
function isAccessTokenExpired() {
  const t = auth.accessToken;
  if (!t) return false;
  try {
    const p = JSON.parse(atob(t.split(".")[1].replace(/-/g, "+").replace(/_/g, "/")));
    return !p.exp || p.exp * 1000 <= Date.now();
  } catch { return false; }
}

async function request(method, path, { body, isForm = false, auth: needAuth = true } = {}) {
  // 토큰이 이미 만료됐다면 요청 전에 한 번만 갱신한다.
  // (안 하면 첫 요청이 401을 받고 재시도되어 동일 요청이 2번 나가는 것처럼 보인다.)
  if (needAuth && auth.accessToken && isAccessTokenExpired()) {
    await tryRefresh();
  }

  const headers = {};
  if (!isForm && body !== undefined) headers["Content-Type"] = "application/json";
  if (needAuth && auth.accessToken) headers["Authorization"] = `Bearer ${auth.accessToken}`;

  const init = { method, headers };
  if (body !== undefined) init.body = isForm ? body : JSON.stringify(body);

  let res = await fetch(`${BASE}${path}`, init);

  if (res.status === 401 && needAuth) {
    const ok = await tryRefresh();
    if (ok) {
      headers["Authorization"] = `Bearer ${auth.accessToken}`;
      res = await fetch(`${BASE}${path}`, init);
    } else {
      auth.clear();
      if (location.hash !== "#/login" && location.hash !== "#/signup") {
        location.hash = "#/login";
      }
    }
  }

  const ct = res.headers.get("content-type") || "";
  const parsed = ct.includes("application/json") ? await res.json().catch(() => null) : await res.text();

  if (!res.ok) {
    const msg = (parsed && typeof parsed === "object" && (parsed.message || parsed.error)) || res.statusText;
    throw new ApiError(msg || "요청 실패", res.status, parsed);
  }

  // Unwrap CommonResponse { message, data } — most endpoints use this shape.
  if (parsed && typeof parsed === "object" && "data" in parsed && "message" in parsed) {
    return parsed.data;
  }
  return parsed;
}

export const api = {
  get: (path, opts) => request("GET", path, opts),
  post: (path, body, opts) => request("POST", path, { body, ...opts }),
  put: (path, body, opts) => request("PUT", path, { body, ...opts }),
  patch: (path, body, opts) => request("PATCH", path, { body, ...opts }),
  del: (path, opts) => request("DELETE", path, opts),

  async uploadImage(file) {
    // 업로드 전 최대 1080px로 축소해 대용량 원본 전송을 막는다. 실패하면 원본 그대로.
    const blob = await resizeImage(file, 1080).catch(() => file);
    const name = blob === file ? file.name : file.name.replace(/\.\w+$/, "") + ".jpg";
    const fd = new FormData();
    fd.append("file", blob, name);
    return request("POST", "/files/image", { body: fd, isForm: true });
  },
};

// 캔버스로 이미지를 maxDim 이내로 축소한 Blob 반환. 이미 작거나 비이미지면 원본 반환.
function resizeImage(file, maxDim) {
  return new Promise((resolve, reject) => {
    if (!file.type?.startsWith("image/") || file.type === "image/gif") return resolve(file);
    const url = URL.createObjectURL(file);
    const img = new Image();
    img.onload = () => {
      URL.revokeObjectURL(url);
      const { width, height } = img;
      if (Math.max(width, height) <= maxDim) return resolve(file);
      const scale = maxDim / Math.max(width, height);
      const canvas = document.createElement("canvas");
      canvas.width = Math.round(width * scale);
      canvas.height = Math.round(height * scale);
      canvas.getContext("2d").drawImage(img, 0, 0, canvas.width, canvas.height);
      canvas.toBlob(b => (b ? resolve(b) : reject(new Error("resize failed"))), "image/jpeg", 0.9);
    };
    img.onerror = () => { URL.revokeObjectURL(url); reject(new Error("image load failed")); };
    img.src = url;
  });
}

export { ApiError };
