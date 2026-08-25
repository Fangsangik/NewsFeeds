import { api } from "./api.js";
import { auth } from "./store.js";
import { el, toast } from "./ui.js";

export function renderLogin(root) {
  root.innerHTML = "";

  const errEl = el("div", { class: "error" });
  const emailIn = el("input", { type: "email", placeholder: "이메일", autocomplete: "email" });
  const pwIn = el("input", { type: "password", placeholder: "비밀번호", autocomplete: "current-password" });
  const submit = el("button", { class: "btn-primary", type: "submit" }, "로그인");

  async function onSubmit(e) {
    e.preventDefault();
    errEl.textContent = "";
    submit.disabled = true;
    try {
      const data = await api.post("/auth/login", {
        email: emailIn.value.trim(),
        password: pwIn.value,
      }, { auth: false });
      auth.setSession({
        id: data.id,
        accessToken: data.accessToken,
        refreshToken: data.refreshToken,
        email: emailIn.value.trim(),
      });
      location.hash = "#/";
    } catch (err) {
      errEl.textContent = err.message || "로그인에 실패했습니다.";
    } finally {
      submit.disabled = false;
    }
  }

  const form = el("form", { onsubmit: onSubmit, action: "javascript:void(0)", method: "post" }, [
    el("div", { class: "field" }, emailIn),
    el("div", { class: "field" }, pwIn),
    errEl,
    submit,
  ]);

  root.appendChild(
    el("div", { class: "auth-wrap" }, [
      el("div", { class: "auth-card" }, [
        el("div", { class: "brand" }, "Newsfeed"),
        form,
        el("div", { class: "divider" }, "OR"),
        el("button", {
          class: "kakao-btn",
          type: "button",
          onclick: () => { window.location.href = "/kakao/authorize"; },
        }, "카카오로 로그인"),
        el("a", { class: "muted-link", href: "#/signup" }, "계정 만들기"),
      ]),
      el("div", { class: "auth-foot" }, [
        document.createTextNode("계정이 없으신가요? "),
        el("a", { href: "#/signup" }, "가입하기"),
      ]),
    ])
  );
}

export function renderSignup(root) {
  root.innerHTML = "";

  const generalErrEl = el("div", { class: "error" });
  const inputs = {
    email: el("input", {
      type: "email",
      placeholder: "이메일 (예: name@example.com)",
      required: true,
      pattern: "[a-zA-Z0-9_!#$%&'*+/=?`{|}~^.-]+@[a-zA-Z0-9.-]+\\.[a-zA-Z0-9.-]+",
      title: "유효한 이메일 형식이어야 합니다 (예: name@example.com)",
      autocomplete: "email",
    }),
    name: el("input", { type: "text", placeholder: "이름", required: true, autocomplete: "name" }),
    password: el("input", {
      type: "password",
      placeholder: "비밀번호 (영문·숫자·특수문자 8자 이상)",
      required: true,
      minlength: "8",
      pattern: "^(?=.*[A-Za-z])(?=.*\\d)(?=.*[^A-Za-z\\d]).{8,}$",
      title: "영문, 숫자, 특수문자를 모두 포함해 8자 이상",
      autocomplete: "new-password",
    }),
    phoneNumber: el("input", { type: "tel", placeholder: "휴대폰 번호 (예: 010-1234-5678)", required: true, autocomplete: "tel" }),
    address: el("input", { type: "text", placeholder: "주소", required: true, autocomplete: "street-address" }),
    age: el("input", { type: "number", placeholder: "나이", min: "1", max: "150", required: true }),
  };

  // Per-field inline error elements next to each input.
  const errs = {};
  const fields = Object.entries(inputs).map(([key, input]) => {
    const err = el("div", { class: "field-error" });
    errs[key] = err;
    // Clear inline error as soon as user edits the field.
    input.addEventListener("input", () => {
      if (err.textContent) {
        err.textContent = "";
        input.classList.remove("invalid");
      }
    });
    return el("div", { class: "field" }, [input, err]);
  });

  function clearAllErrors() {
    generalErrEl.textContent = "";
    Object.entries(errs).forEach(([key, err]) => {
      err.textContent = "";
      inputs[key].classList.remove("invalid");
    });
  }

  function showFieldError(field, message) {
    const input = inputs[field];
    const err = errs[field];
    if (!input || !err) {
      // Unknown field name from the server — surface as a general error.
      generalErrEl.textContent = generalErrEl.textContent
        ? `${generalErrEl.textContent}\n${message}`
        : message;
      return;
    }
    input.classList.add("invalid");
    err.textContent = message;
  }

  const submit = el("button", { class: "btn-primary", type: "submit" }, "가입하기");

  async function onSubmit(e) {
    e.preventDefault();
    clearAllErrors();
    submit.disabled = true;
    try {
      const payload = {
        name: inputs.name.value.trim(),
        email: inputs.email.value.trim(),
        password: inputs.password.value,
        phoneNumber: inputs.phoneNumber.value.trim(),
        address: inputs.address.value.trim(),
        age: Number(inputs.age.value) || 0,
        role: "USER",
      };
      await api.post("/members/signup", payload, { auth: false });
      toast("가입 완료! 로그인하세요.");
      location.hash = "#/login";
    } catch (err) {
      if (err.fieldErrors && Object.keys(err.fieldErrors).length) {
        Object.entries(err.fieldErrors).forEach(([k, msg]) => showFieldError(k, msg));
        // Focus the first invalid field so user can fix it right away.
        const firstBadKey = Object.keys(err.fieldErrors).find(k => inputs[k]);
        if (firstBadKey) inputs[firstBadKey].focus();
      } else {
        generalErrEl.textContent = err.message || "가입에 실패했습니다.";
      }
    } finally {
      submit.disabled = false;
    }
  }

  root.appendChild(
    el("div", { class: "auth-wrap" }, [
      el("div", { class: "auth-card" }, [
        el("div", { class: "brand" }, "Newsfeed"),
        el("p", { class: "muted" }, "친구들의 사진과 동영상을 보려면 가입하세요."),
        el("form", { onsubmit: onSubmit, action: "javascript:void(0)", method: "post" }, [...fields, generalErrEl, submit]),
      ]),
      el("div", { class: "auth-foot" }, [
        document.createTextNode("계정이 있으신가요? "),
        el("a", { href: "#/login" }, "로그인"),
      ]),
    ])
  );
}

export function logout() {
  const token = auth.accessToken;
  if (token) {
    api.post("/auth/logout", undefined).catch(() => { /* best effort */ });
  }
  auth.clear();
  location.hash = "#/login";
}
