<script setup>
import { ref } from 'vue';
import axios from 'axios';
import { useRouter } from 'vue-router';

const username = ref("");
const router = useRouter();

// ログイン（または新規登録）処理
const login = async () => {
  if (!username.value) {
    alert("ユーザー名を入力してください");
    return;
  }

  try {
    // バックエンドの /api/users/login にリクエスト
    const response = await axios.post("http://localhost:8080/api/users/login", {
      username: username.value
    });

    const user = response.data;

    // 【重要】ブラウザの「localStorage」にユーザー情報を保存する（これでリロードしても消えません）
    localStorage.setItem("app_token", user.token);
    localStorage.setItem("app_userId", user.id);
    localStorage.setItem("app_username", user.username);

    // 【重要】以降の axios 通信すべてに、自動的に Authorization ヘッダーをつける設定
    axios.defaults.headers.common['Authorization'] = user.token;

    alert(`${user.username} さん、ようこそ！`);
    
    // 目標一覧画面（home）へ画面遷移する
    router.push({ name: 'home' });

  } catch (error) {
    alert("ログインに失敗しました");
    console.error(error);
  }
};
</script>

<template>
  <main class="login-container">
    <h1>目標管理アプリ</h1>
    <div class="login-box">
      <h2>ログイン</h2>
      <p>ユーザー名を入力して開始してください</p>
      
      <input 
        type="text" 
        v-model="username" 
        placeholder="例: tarou" 
        @keyup.enter="login"
      >
      <button @click="login">はじめる</button>
    </div>
  </main>
</template>

<style scoped>
.login-container {
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  height: 80vh;
}
.login-box {
  border: 1px solid #ccc;
  padding: 30px;
  border-radius: 12px;
  background-color: #f9f9f9;
  text-align: center;
  box-shadow: 0 4px 15px rgba(0,0,0,0.1);
}
input {
  padding: 10px;
  font-size: 16px;
  margin-bottom: 15px;
  width: 80%;
  border-radius: 6px;
  border: 1px solid #ccc;
}
button {
  padding: 10px 20px;
  font-size: 16px;
  background-color: #4CAF50;
  color: white;
  border: none;
  border-radius: 6px;
  cursor: pointer;
  font-weight: bold;
}
button:hover {
  background-color: #45a049;
}
</style>