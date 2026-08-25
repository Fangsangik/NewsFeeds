// Lightweight localStorage wrapper for auth + per-user like state.

const KEY = {
  ACCESS: "nf.accessToken",
  REFRESH: "nf.refreshToken",
  ME_ID: "nf.meId",
  ME_EMAIL: "nf.meEmail",
};

export const auth = {
  setSession({ id, accessToken, refreshToken, email }) {
    if (accessToken) localStorage.setItem(KEY.ACCESS, accessToken);
    if (refreshToken) localStorage.setItem(KEY.REFRESH, refreshToken);
    if (id != null) localStorage.setItem(KEY.ME_ID, String(id));
    if (email) localStorage.setItem(KEY.ME_EMAIL, email);
  },
  setAccessToken(token) {
    if (token) localStorage.setItem(KEY.ACCESS, token);
  },
  clear() {
    [KEY.ACCESS, KEY.REFRESH, KEY.ME_ID, KEY.ME_EMAIL].forEach(k => localStorage.removeItem(k));
  },
  get accessToken() { return localStorage.getItem(KEY.ACCESS); },
  get refreshToken() { return localStorage.getItem(KEY.REFRESH); },
  get meId() {
    const v = localStorage.getItem(KEY.ME_ID);
    return v ? Number(v) : null;
  },
  get meEmail() { return localStorage.getItem(KEY.ME_EMAIL); },
  get isLoggedIn() { return !!localStorage.getItem(KEY.ACCESS); },
};

// Local "did I like this feed" cache. The backend's like API increments/decrements
// a count without telling us whether *I* already liked it, so we approximate
// it client-side per user. Not authoritative, but matches Insta-style UX.
const likedKey = () => `nf.liked.${auth.meId ?? "anon"}`;

export const likes = {
  set(feedId, liked) {
    const set = this._read();
    if (liked) set.add(Number(feedId)); else set.delete(Number(feedId));
    localStorage.setItem(likedKey(), JSON.stringify([...set]));
  },
  has(feedId) {
    return this._read().has(Number(feedId));
  },
  _read() {
    try {
      return new Set(JSON.parse(localStorage.getItem(likedKey()) || "[]"));
    } catch {
      return new Set();
    }
  },
};
