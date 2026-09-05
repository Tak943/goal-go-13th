import { createApp } from 'vue'
import App from './App.vue'
import router from './router'
import axios from 'axios' // 【追加】axiosをインポート

// 【追加】すべての通信の基本URLをここで一括設定！
// ※これで、各Vueファイルに書いていた axios.defaults.baseURL = ... は不要になります！
axios.defaults.baseURL = 'http://localhost:8080';

// 【追加】アプリ起動時に、ブラウザの記憶からトークンを取り出してセットする
const token = localStorage.getItem('app_token');
if (token) {
  axios.defaults.headers.common['Authorization'] = token;
}

const app = createApp(App)
app.use(router)
app.mount('#app')