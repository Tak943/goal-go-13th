<script setup>
import { ref } from 'vue';
import axios from 'axios';
import { useRouter } from 'vue-router';

const username = ref("");
const router = useRouter();

// Toast
const toast = ref({
  show: false,
  message: '',
  type: 'error'
});

let toastTimer = null;

const showToast = (message, type = 'error') => {
  toast.value = {
    show: true,
    message,
    type
  };

  clearTimeout(toastTimer);

  toastTimer = setTimeout(() => {
    toast.value.show = false;
  }, 3000);
};

// ログイン（または新規登録）処理
const login = async () => {
  if (!username.value.trim()) {
    showToast("ユーザー名を入力してください");
    return;
  }

  try {
    const response = await axios.post(
      "/api/users/login",
      {
        username: username.value
      }
    );

    const user = response.data;

    localStorage.setItem("app_token", user.token);
    localStorage.setItem("app_userId", user.id);
    localStorage.setItem("app_username", user.username);

    axios.defaults.headers.common['Authorization'] = user.token;

    router.push({ name: 'home' });

  } catch (error) {
    showToast("ログインに失敗しました。もう一度お試しください。");
    alert("エラー原因: " + error.message);
    console.error(error);
  }
};
</script>

<template>
  <main class="login-container">

    <!-- ロゴ・アプリ名 -->
    <div class="brand">
      <div class="brand-icon">
        ✕
      </div>

      <h1>Goal-Go-13th 🎯</h1>

      <p>
        小さな一歩を、毎日の記録に。
      </p>
    </div>

    <!-- ログインカード -->
    <section class="login-box">

      <div class="login-heading">
        <span>WELCOME</span>
        <h2>はじめよう</h2>
        <p>
          ユーザー名を入力して<br>
          あなたの目標を始めましょう。
        </p>
      </div>

      <form @submit.prevent="login">

        <label for="username">
          ユーザー名
        </label>

        <input
          id="username"
          type="text"
          v-model="username"
          placeholder="例：たろう"
          autocomplete="username"
        >

        <button
          type="submit"
          class="login-button"
        >
          はじめる
          <span>→</span>
        </button>

      </form>

      <p class="login-note">
        初めての方は、入力したユーザー名で<br>
        自動的にアカウントが作成されます。
      </p>

    </section>

    <!-- Toast -->
    <Transition name="toast">
      <div
        v-if="toast.show"
        class="toast"
        :class="`toast-${toast.type}`"
      >
        <span class="toast-icon">
          !
        </span>

        <span class="toast-message">
          {{ toast.message }}
        </span>

        <button
          class="toast-close"
          @click="toast.show = false"
          aria-label="閉じる"
        >
          ×
        </button>
      </div>
    </Transition>

  </main>
</template>

<style scoped>

* {
  box-sizing: border-box;
}

/* =========================
   Login page
========================= */

.login-container {
  min-height: 100vh;

  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;

  padding: 32px 16px;

  background:
    radial-gradient(
      circle at top,
      #edf7f0 0%,
      #f6f8f7 45%,
      #f6f8f7 100%
    );

  color: #202522;
}

/* =========================
   Brand
========================= */

.brand {
  margin-bottom: 28px;

  text-align: center;
}

.brand-icon {
  width: 56px;
  height: 56px;

  margin: 0 auto 14px;

  display: flex;
  align-items: center;
  justify-content: center;

  border-radius: 18px;

  background: #4f9569;
  color: white;

  font-size: 30px;
  font-weight: 800;

  box-shadow:
    0 8px 20px rgba(79, 149, 105, 0.25);
}

.brand h1 {
  margin: 0;

  font-size: 25px;
  font-weight: 800;
  letter-spacing: -0.02em;
}

.brand p {
  margin: 8px 0 0;

  color: #7b857f;
  font-size: 13px;
}

/* =========================
   Login box
========================= */

.login-box {
  width: min(100%, 400px);

  padding: 32px 28px;

  background: white;

  border: 1px solid #edf0ee;
  border-radius: 24px;

  box-shadow:
    0 12px 40px rgba(32, 37, 34, 0.08);
}

/* =========================
   Heading
========================= */

.login-heading {
  margin-bottom: 26px;
}

.login-heading > span {
  display: block;

  margin-bottom: 7px;

  font-size: 10px;
  font-weight: 800;
  letter-spacing: 0.14em;

  color: #72a487;
}

.login-heading h2 {
  margin: 0;

  font-size: 26px;
  font-weight: 800;
}

.login-heading p {
  margin: 9px 0 0;

  color: #7b857f;

  font-size: 13px;
  line-height: 1.7;
}

/* =========================
   Form
========================= */

form {
  display: flex;
  flex-direction: column;
}

label {
  margin-bottom: 8px;

  font-size: 13px;
  font-weight: 700;

  color: #4e5651;
}

input {
  width: 100%;
  height: 52px;

  padding: 0 15px;

  border: 1.5px solid #dfe5e1;
  border-radius: 12px;

  background: #fafbfa;

  color: #202522;

  font-size: 16px;

  outline: none;

  transition:
    border-color 0.15s,
    box-shadow 0.15s,
    background 0.15s;
}

input::placeholder {
  color: #b2b8b4;
}

input:focus {
  border-color: #6ea680;

  background: white;

  box-shadow:
    0 0 0 4px rgba(79, 149, 105, 0.10);
}

/* =========================
   Login button
========================= */

.login-button {
  width: 100%;
  height: 52px;

  margin-top: 14px;

  display: flex;
  align-items: center;
  justify-content: center;
  gap: 8px;

  border: none;
  border-radius: 12px;

  background: #4f9569;
  color: white;

  font-size: 15px;
  font-weight: 800;

  cursor: pointer;

  box-shadow:
    0 6px 14px rgba(79, 149, 105, 0.22);

  transition:
    background 0.15s,
    transform 0.1s,
    box-shadow 0.15s;
}

.login-button span {
  font-size: 20px;
  line-height: 1;

  transition: transform 0.15s;
}

.login-button:hover {
  background: #427f59;

  box-shadow:
    0 8px 18px rgba(79, 149, 105, 0.28);
}

.login-button:hover span {
  transform: translateX(3px);
}

.login-button:active {
  transform: scale(0.98);
}

/* =========================
   Note
========================= */

.login-note {
  margin: 20px 0 0;

  text-align: center;

  font-size: 11px;
  line-height: 1.7;

  color: #a0a6a2;
}

/* =========================
   Toast
========================= */

.toast {
  position: fixed;
  z-index: 2000;

  left: 50%;
  bottom: 24px;

  transform: translateX(-50%);

  width: min(calc(100% - 32px), 420px);

  display: flex;
  align-items: center;
  gap: 10px;

  padding: 14px 16px;

  border-radius: 14px;

  background: #262b28;
  color: white;

  font-size: 13px;
  line-height: 1.4;

  box-shadow:
    0 10px 30px rgba(0, 0, 0, 0.2);
}

.toast-icon {
  flex-shrink: 0;

  width: 24px;
  height: 24px;

  display: flex;
  align-items: center;
  justify-content: center;

  border-radius: 50%;

  background: rgba(255, 255, 255, 0.15);

  font-weight: 800;
}

.toast-message {
  flex: 1;
}

.toast-close {
  flex-shrink: 0;

  width: 28px;
  height: 28px;

  border: none;
  background: transparent;

  color: rgba(255, 255, 255, 0.7);

  font-size: 20px;
  line-height: 1;

  cursor: pointer;
}

.toast-close:hover {
  color: white;
}

/* =========================
   Toast animation
========================= */

.toast-enter-active,
.toast-leave-active {
  transition: all 0.25s ease;
}

.toast-enter-from,
.toast-leave-to {
  opacity: 0;

  transform:
    translate(-50%, 15px);
}

/* =========================
   Mobile
========================= */

@media (max-width: 600px) {

  .login-container {
    justify-content: center;

    padding:
      24px 16px
      calc(24px + env(safe-area-inset-bottom));
  }

  .brand {
    margin-bottom: 22px;
  }

  .brand-icon {
    width: 50px;
    height: 50px;

    border-radius: 15px;

    font-size: 27px;
  }

  .brand h1 {
    font-size: 22px;
  }

  .login-box {
    padding: 26px 20px;

    border-radius: 20px;
  }

  .login-heading h2 {
    font-size: 24px;
  }

  .toast {
    bottom:
      calc(16px + env(safe-area-inset-bottom));
  }
}

</style>