import { createRouter, createWebHistory } from 'vue-router';
import GoalListView from '../views/GoalListView.vue';
import CalendarView from '../views/CalendarView.vue';
import LoginView from '../views/LoginView.vue'; // 【追加】ログイン画面をインポート

const router = createRouter({
  history: createWebHistory(import.meta.env.BASE_URL),
  routes: [
    // 【追加】ログイン画面のルート
    {
      path: '/login',
      name: 'login',
      component: LoginView
    },
    {
      path: '/',
      name: 'home',
      component: GoalListView,
      // 【追加】この画面は「ログイン必須」という目印をつける
      meta: { requiresAuth: true } 
    },
    {
      path: '/goals/:goalId/calendar',
      name: 'calendar',
      component: CalendarView,
      // 【追加】この画面も「ログイン必須」
      meta: { requiresAuth: true } 
    }
  ],
})

// --- 【ここから追加】ページ移動のたびにチェックする「関所」 ---
router.beforeEach((to, from, next) => {
  // ブラウザの記憶（localStorage）からトークンを取り出す
  const token = localStorage.getItem('app_token');

  // ① 行き先が「ログイン必須（requiresAuthがtrue）」なのに、トークンを持っていない場合
  if (to.matched.some(record => record.meta.requiresAuth) && !token) {
    // ログイン画面に強制リダイレクト！
    next({ name: 'login' });
  } 
  // ② それ以外（トークンを持っている、または行き先がログイン画面）の場合
  else {
    // そのまま通す
    next();
  }
});

export default router;