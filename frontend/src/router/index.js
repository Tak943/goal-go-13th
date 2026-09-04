import { createRouter, createWebHistory } from 'vue-router';
import GoalListView from '../views/GoalListView.vue';
import CalendarView from '../views/CalendarView.vue';

const router = createRouter({
  history: createWebHistory(import.meta.env.BASE_URL),
  routes: [
    {
      //トップページ（http://localhost:5173/）のときは、目標一覧を表示
      path: '/',
      name: 'home',
      component: GoalListView
    },
    {
      //カレンダー画面（http://localhost:5173/goals/1/calendar などのとき）
      path: '/goals/:goalId/calendar',
      name: 'calendar',
      component: CalendarView
    }
  ],
})

export default router
