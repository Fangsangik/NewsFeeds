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

async function request(method, path, { body, isForm = false, auth: needAuth = true } = {}) {
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
    const fd = new FormData();
    fd.append("file", file);
    return request("POST", "/files/image", { body: fd, isForm: true });
  },
};

export { ApiError };
