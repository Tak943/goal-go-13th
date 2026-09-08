<script setup>
import { ref, onMounted, nextTick, computed } from 'vue';
import axios from 'axios';
import { useRoute, useRouter } from 'vue-router'; // 【変更】useRouterを追加
import TodoView from './TodoView.vue';

const route = useRoute();
const router = useRouter(); // 【追加】画面遷移用
const goalId = route.params.goalId;
const goal = ref({});

const markedMap = ref({});

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

// カレンダーの表示用の年月（初期値は今日）
const viewYear = ref(new Date().getFullYear());
const viewMonth = ref(new Date().getMonth());

// --- 【追加】ログアウト関数 ---
const isLogoutModalOpen = ref(false);

const logout = () => {
  isLogoutModalOpen.value = true;
};

const cancelLogout = () => {
  isLogoutModalOpen.value = false;
};

const executeLogout = () => {
  localStorage.removeItem('app_token');
  localStorage.removeItem('app_userId');
  localStorage.removeItem('app_username');

  delete axios.defaults.headers.common['Authorization'];

  isLogoutModalOpen.value = false;

  router.push({ name: 'login' });
};

onMounted(async () => {
  // 【変更】他人のデータを見ようとした時の403エラーをキャッチするために try-catch を追加
  try {
    const response = await axios.get(`/api/goals/${goalId}`);
    goal.value = response.data;

    // 目標の開始日に合わせてカレンダーの初期表示を切り替える
    if (goal.value.startDate) {
      const start = new Date(goal.value.startDate);
      viewYear.value = start.getFullYear();
      viewMonth.value = start.getMonth();
    }

    const markResponse = await axios.get(`/api/goals/${goalId}/marks`);
    const map = {};
    for(let i = 0; i < markResponse.data.length; i++){
      map[markResponse.data[i].markedDate] = true;
    }
    markedMap.value = map;
    
  } catch (error) {
    showToast("データの取得に失敗しました。目標が存在しないか、アクセスできません。");
    console.error(error);

    setTimeout(() => {
      router.push({ name: 'home' });
    }, 1800);
  }
});

// --- カレンダーの月切り替え処理 ---
const prevMonth = () => {
  if (viewMonth.value === 0) {
    viewMonth.value = 11;
    viewYear.value--;
  } else {
    viewMonth.value--;
  }
};
const nextMonth = () => {
  if (viewMonth.value === 11) {
    viewMonth.value = 0;
    viewYear.value++;
  } else {
    viewMonth.value++;
  }
};

const calendarGrid = computed(() => {
  if (!goal.value.startDate) return []; 

  const firstDayOfWeek = new Date(viewYear.value, viewMonth.value, 1).getDay();
  const lastDate = new Date(viewYear.value, viewMonth.value + 1, 0).getDate();
  
  const days = [];
  
  for (let i = 0; i < firstDayOfWeek; i++) {
    days.push(null);
  }
  
  for (let i = 1; i <= lastDate; i++) {
    const y = viewYear.value;
    const m = String(viewMonth.value + 1).padStart(2, '0');
    const d = String(i).padStart(2, '0');
    const apiDate = `${y}-${m}-${d}`;
    
    const isWithinRange = (apiDate >= goal.value.startDate) && (apiDate <= goal.value.targetDate);
    
    //今日が「開始日」または「目標日」と完全に一致するか判定
    const isStartDay = (apiDate === goal.value.startDate);
    const isTargetDay = (apiDate === goal.value.targetDate);

    days.push({
      apiDate: apiDate,
      displayDate: `${viewMonth.value + 1}/${i}`,
      dayNum: i,
      isMarked: !!markedMap.value[apiDate],
      isWithinRange: isWithinRange,
      
      isStartDay: isStartDay, 
      isTargetDay: isTargetDay
    });
  }
  
  return days;
});


// --- お絵かきモーダル関連の変数 ---
const isModalOpen = ref(false);
const selectedDay = ref(null);
const canvasRef = ref(null);
let ctx = null;
const isDrawing = ref(false);
const drawnSegments = ref([]); 
let lastPos = null; 

// モーダルを開く＆ペンの準備
const openModal = async (day) => {
  if (!day || !day.isWithinRange) return; // 空白や期間外なら何もしない
  if (day.isMarked) {
    showToast("この日はすでに記録済みです。", "info");
    return;
  }
  selectedDay.value = day;
  isModalOpen.value = true;
  
  await nextTick();
  
  const canvas = canvasRef.value;
  ctx = canvas.getContext('2d');
  ctx.lineWidth = 20;
  ctx.lineCap = 'round';
  ctx.strokeStyle = '#ff5252';
  
  clearCanvas();
};

const closeModal = () => {
  isModalOpen.value = false;
  selectedDay.value = null;
};

const clearCanvas = () => {
  if (ctx && canvasRef.value) {
    ctx.clearRect(0, 0, canvasRef.value.width, canvasRef.value.height);
  }
  drawnSegments.value = [];
};

// --- お絵かき＆当たり判定ロジック ---
const getCoordinates = (e) => {
  if (e.touches && e.touches.length > 0) {
    const rect = canvasRef.value.getBoundingClientRect();
    return {
      x: e.touches[0].clientX - rect.left,
      y: e.touches[0].clientY - rect.top
    };
  }
  return { x: e.offsetX, y: e.offsetY };
};

const startDrawing = (e) => {
  isDrawing.value = true;
  ctx.beginPath();
  const pos = getCoordinates(e);
  ctx.moveTo(pos.x, pos.y); 
  lastPos = pos;
};

const draw = (e) => {
  if (!isDrawing.value) return; 
  const pos = getCoordinates(e);
  ctx.lineTo(pos.x, pos.y);
  ctx.stroke();
  if (lastPos) {
    drawnSegments.value.push({ p1: lastPos, p2: pos });
  }
  lastPos = pos;
};

const stopDrawing = () => {
  isDrawing.value = false;
  lastPos = null;
};

// 交点判定
const ccw = (A, B, C) => {
  return (C.y - A.y) * (B.x - A.x) > (B.y - A.y) * (C.x - A.x);
};
const isIntersect = (A, B, C, D) => {
  return ccw(A, C, D) !== ccw(B, C, D) && ccw(A, B, C) !== ccw(A, B, D);
};

// 最終判定とAPI送信
const saveMark = async () => {
  let hasIntersection = false;
  const segments = drawnSegments.value;

  for (let i = 0; i < segments.length; i++) {
    for (let j = i + 10; j < segments.length; j++) {
      if (isIntersect(segments[i].p1, segments[i].p2, segments[j].p1, segments[j].p2)) {
        hasIntersection = true;
        break;
      }
    }
    if (hasIntersection) break;
  }
  
  if (!hasIntersection) {
    showToast("✕になっていません。2本の線が交差するように描いてください。");
    return;
  }
  
  try {
    await axios.post(`/api/goals/${goalId}/marks`, {
      "markedDate": selectedDay.value.apiDate
    });
    
    // 辞書のデータを更新することで、自動的にカレンダーの表示も変わる！
    markedMap.value[selectedDay.value.apiDate] = true;
    closeModal();
    
  } catch (error) {
    showToast("記録に失敗しました。時間をおいてもう一度試してください。");
    console.error(error);
  }
};
</script>

<template>
  <main class="calendar-page">

    <!-- ヘッダー -->
    <header class="app-header">
      <button class="back-button" @click="router.push({ name: 'home' })">
        ← 一覧
      </button>

      <button @click="logout" class="logout-btn">
        ログアウト
      </button>
      <!-- ログアウト確認モーダル -->
<div
  v-if="isLogoutModalOpen"
  class="confirm-overlay"
  @click.self="cancelLogout"
>
  <div class="confirm-modal">

    <div class="confirm-icon">
      ↪
    </div>

    <h2>ログアウトしますか？</h2>

    <p>
      現在のアカウントからログアウトします。
    </p>

    <div class="confirm-actions">

      <button
        class="confirm-cancel"
        @click="cancelLogout"
      >
        キャンセル
      </button>

      <button
        class="confirm-logout"
        @click="executeLogout"
      >
        ログアウト
      </button>

    </div>

  </div>
</div>
    </header>

    <!-- 目標情報 -->
    <section class="goal-info">

      <div class="goal-title-row">
        <div>
          <span class="goal-label">MY GOAL</span>
          <h1>{{ goal.title }}</h1>
        </div>
      </div>

      <div class="goal-period">
        <div class="period-item">
          <span class="period-label">START</span>
          <span>{{ goal.startDate }}</span>
        </div>

        <div class="period-arrow">→</div>

        <div class="period-item">
          <span class="period-label">GOAL</span>
          <span>{{ goal.targetDate }}</span>
        </div>
      </div>

    </section>

    <!-- カレンダー -->
    <section class="calendar-container">

      <div class="cal-header">

        <button
          class="month-button"
          @click="prevMonth"
          aria-label="前の月"
        >
          ‹
        </button>

        <div class="month-title">
          <span>{{ viewYear }}</span>
          <strong>{{ viewMonth + 1 }}月</strong>
        </div>

        <button
          class="month-button"
          @click="nextMonth"
          aria-label="次の月"
        >
          ›
        </button>

      </div>

      <!-- 曜日 -->
      <div class="cal-weekdays">
        <span class="sun">日</span>
        <span>月</span>
        <span>火</span>
        <span>水</span>
        <span>木</span>
        <span>金</span>
        <span class="sat">土</span>
      </div>

      <!-- 日付 -->
      <div class="cal-grid">

        <div
          v-for="(day, index) in calendarGrid"
          :key="day ? day.apiDate : index"
          class="day-box"
          :class="{
            'is-empty': !day,
            'out-of-range': day && !day.isWithinRange,
            'marked': day && day.isMarked,
            'start-day': day && day.isStartDay,
            'target-day': day && day.isTargetDay
          }"
          @click="openModal(day)"
        >

          <template v-if="day">

            <span class="date-text">
              {{ day.dayNum }}
            </span>

            <!-- 開始日 -->
            <span
              v-if="day.isStartDay"
              class="day-badge start-badge"
            >
              START
            </span>

            <!-- 目標日 -->
            <span
              v-if="day.isTargetDay"
              class="day-badge target-badge"
            >
              GOAL
            </span>

            <!-- 達成済み -->
            <div
              v-if="day.isMarked"
              class="mark-circle"
            >
              ✕
            </div>

          </template>

        </div>

      </div>

      <!-- 凡例 -->
      <div class="calendar-legend">
        <div>
          <span class="legend-mark achieved"></span>
          <span>記録済み</span>
        </div>

        <div>
          <span class="legend-mark start"></span>
          <span>開始日</span>
        </div>

        <div>
          <span class="legend-mark target"></span>
          <span>目標日</span>
        </div>
      </div>

    </section>

    <!-- Todo -->
    <TodoView :goal-id="goalId" />

    <!-- お絵かきモーダル -->
    <div
      v-if="isModalOpen"
      class="modal-overlay"
      @click.self="closeModal"
    >

      <div class="modal-content">

        <div class="modal-header">
          <div>
            <span class="modal-label">DAILY RECORD</span>
            <h2>{{ selectedDay.displayDate }}</h2>
          </div>

          <button
            class="close-button"
            @click="closeModal"
          >
            ×
          </button>
        </div>

        <p class="modal-description">
          今日もよく頑張った！お疲れ様です！！
        </p>

        <div class="canvas-container">

          <div class="guide-x">×</div>

          <canvas
            ref="canvasRef"
            width="300"
            height="300"
            class="draw-canvas"
            @mousedown="startDrawing"
            @mousemove="draw"
            @mouseup="stopDrawing"
            @mouseleave="stopDrawing"
            @touchstart="startDrawing"
            @touchmove="draw"
            @touchend="stopDrawing"
          ></canvas>

        </div>

        <p class="draw-hint">
          2本の線が交差するように「×」を描いてください
        </p>

        <div class="modal-actions">

          <button
            class="secondary-btn"
            @click="clearCanvas"
          >
            描き直す
          </button>

          <button
            class="cancel-btn"
            @click="closeModal"
          >
            キャンセル
          </button>

          <button
            class="save-btn"
            @click="saveMark"
          >
            記録する
          </button>

        </div>

      </div>

    </div>

    <!-- Toast -->
    <Transition name="toast">
      <div
        v-if="toast.show"
        class="toast"
        :class="`toast-${toast.type}`"
      >
        <span class="toast-icon">
          {{ toast.type === 'info' ? 'ⓘ' : '!' }}
        </span>

        <span>{{ toast.message }}</span>

        <button
          class="toast-close"
          @click="toast.show = false"
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

.calendar-page {
  min-height: 100vh;
  padding: 24px 16px 48px;
  background: #f6f8f7;
  color: #202522;
}

/* =========================
   Header
========================= */

.app-header {
  max-width: 680px;
  margin: 0 auto 24px;
  display: flex;
  justify-content: space-between;
  align-items: center;
}

.back-button,
.logout-btn {
  border: none;
  background: transparent;
  cursor: pointer;
  font-size: 14px;
  font-weight: 600;
}

.back-button {
  color: #3d7a58;
  padding: 10px 4px;
}

.logout-btn {
  color: #777;
  padding: 10px 4px;
}

.back-button:hover {
  color: #245b3d;
}

.logout-btn:hover {
  color: #333;
}

/* =========================
   Goal information
========================= */

.goal-info {
  max-width: 680px;
  margin: 0 auto 20px;
  padding: 24px;
  background: white;
  border-radius: 20px;
  box-shadow: 0 4px 20px rgba(0, 0, 0, 0.05);
}

.goal-label,
.modal-label {
  display: block;
  margin-bottom: 6px;
  font-size: 11px;
  font-weight: 800;
  letter-spacing: 0.12em;
  color: #72a487;
}

.goal-info h1 {
  margin: 0;
  font-size: clamp(22px, 5vw, 30px);
  line-height: 1.3;
  word-break: break-word;
}

.goal-period {
  display: flex;
  align-items: center;
  gap: 14px;
  margin-top: 20px;
  padding-top: 18px;
  border-top: 1px solid #edf0ee;
}

.period-item {
  display: flex;
  flex-direction: column;
  gap: 3px;
  font-size: 13px;
  color: #555;
}

.period-label {
  font-size: 10px;
  font-weight: 800;
  letter-spacing: 0.08em;
  color: #999;
}

.period-arrow {
  color: #aaa;
  font-size: 18px;
}

/* =========================
   Calendar
========================= */

.calendar-container {
  max-width: 680px;
  margin: 0 auto;
  padding: 20px;
  background: white;
  border-radius: 20px;
  box-shadow: 0 4px 20px rgba(0, 0, 0, 0.05);
}

.cal-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-bottom: 22px;
}

.month-title {
  display: flex;
  align-items: baseline;
  gap: 5px;
}

.month-title span {
  font-size: 14px;
  color: #777;
}

.month-title strong {
  font-size: 22px;
}

.month-button {
  width: 44px;
  height: 44px;
  border: none;
  border-radius: 12px;
  background: #f2f5f3;
  color: #444;
  font-size: 30px;
  line-height: 1;
  cursor: pointer;
  transition: 0.15s;
}

.month-button:hover {
  background: #e5ebe7;
}

.month-button:active {
  transform: scale(0.94);
}

/* =========================
   Weekdays
========================= */

.cal-weekdays {
  display: grid;
  grid-template-columns: repeat(7, 1fr);
  margin-bottom: 7px;
  text-align: center;
}

.cal-weekdays span {
  font-size: 12px;
  font-weight: 700;
  color: #888;
}

.cal-weekdays .sun {
  color: #df7272;
}

.cal-weekdays .sat {
  color: #668ac1;
}

/* =========================
   Calendar grid
========================= */

.cal-grid {
  display: grid;
  grid-template-columns: repeat(7, 1fr);
  gap: 5px;
}

.day-box {
  position: relative;
  aspect-ratio: 1 / 1;
  min-width: 0;
  border: 1px solid #edf0ee;
  border-radius: 12px;
  background: #fff;
  cursor: pointer;

  display: flex;
  align-items: flex-start;
  justify-content: center;

  padding-top: 9px;

  transition:
    transform 0.12s,
    background 0.12s,
    border-color 0.12s;
}

.day-box:not(.is-empty):not(.out-of-range):not(.marked):hover {
  background: #f2f8f4;
  border-color: #a8c9b5;
}

.day-box:not(.is-empty):not(.out-of-range):active {
  transform: scale(0.94);
}

.day-box.is-empty {
  border: none;
  background: transparent;
  cursor: default;
}

.day-box.out-of-range {
  background: #f7f7f7;
  border-color: #f7f7f7;
  color: #d0d0d0;
  cursor: default;
}

.date-text {
  font-size: clamp(13px, 3vw, 16px);
  font-weight: 600;
}

/* =========================
   Marked
========================= */

.day-box.marked {
  background: #eef8f1;
  border-color: #b8d8c2;
}

.mark-circle {
  position: absolute;
  inset: 50% auto auto 50%;
  transform: translate(-50%, -38%);

  width: 34px;
  height: 34px;

  border-radius: 50%;
  background: #4f9569;
  color: white;

  display: flex;
  align-items: center;
  justify-content: center;

  font-size: 20px;
  font-weight: 800;

  box-shadow: 0 3px 8px rgba(79, 149, 105, 0.25);
}

/* =========================
   Start / Goal badges
========================= */

.day-badge {
  position: absolute;
  bottom: 4px;

  padding: 2px 4px;
  border-radius: 4px;

  font-size: 7px;
  font-weight: 800;
  letter-spacing: 0.04em;
}

.start-badge {
  left: 3px;
  color: #d47734;
  background: #fff2e7;
}

.target-badge {
  right: 3px;
  color: #8766bd;
  background: #f2ecfb;
}

/* =========================
   Legend
========================= */

.calendar-legend {
  display: flex;
  justify-content: center;
  flex-wrap: wrap;
  gap: 16px;
  margin-top: 20px;
  padding-top: 16px;
  border-top: 1px solid #edf0ee;
}

.calendar-legend > div {
  display: flex;
  align-items: center;
  gap: 6px;

  font-size: 11px;
  color: #777;
}

.legend-mark {
  width: 9px;
  height: 9px;
  border-radius: 50%;
}

.legend-mark.achieved {
  background: #4f9569;
}

.legend-mark.start {
  background: #e59a62;
}

.legend-mark.target {
  background: #9676c5;
}

/* =========================
   Modal
========================= */

.modal-overlay {
  position: fixed;
  inset: 0;
  z-index: 1000;

  display: flex;
  align-items: center;
  justify-content: center;

  padding: 20px;

  background: rgba(20, 27, 23, 0.55);
  backdrop-filter: blur(5px);
}

.modal-content {
  width: min(100%, 430px);
  max-height: calc(100vh - 40px);
  overflow-y: auto;

  padding: 24px;

  background: white;
  border-radius: 24px;

  box-shadow: 0 20px 60px rgba(0, 0, 0, 0.2);
}

.modal-header {
  display: flex;
  align-items: flex-start;
  justify-content: space-between;
}

.modal-header h2 {
  margin: 0;
  font-size: 24px;
}

.close-button {
  width: 38px;
  height: 38px;

  border: none;
  border-radius: 50%;

  background: #f3f4f3;
  color: #555;

  font-size: 24px;
  cursor: pointer;
}

.modal-description {
  margin: 8px 0 18px;
  color: #777;
  font-size: 14px;
}

/* =========================
   Canvas
========================= */

.canvas-container {
  position: relative;

  width: min(300px, 100%);
  aspect-ratio: 1 / 1;

  margin: 0 auto;

  border-radius: 18px;
  overflow: hidden;

  background:
    linear-gradient(#f1f4f2 1px, transparent 1px),
    linear-gradient(90deg, #f1f4f2 1px, transparent 1px);
  background-size: 30px 30px;
}

.guide-x {
  position: absolute;
  inset: 50% auto auto 50%;
  transform: translate(-50%, -50%);

  font-size: 230px;
  line-height: 1;

  color: #e5e9e6;

  user-select: none;
  pointer-events: none;
  z-index: 1;
}

.draw-canvas {
  position: absolute;
  inset: 0;

  width: 100%;
  height: 100%;

  cursor: crosshair;
  touch-action: none;

  z-index: 2;
}

.draw-hint {
  margin: 12px 0 0;

  text-align: center;

  font-size: 12px;
  line-height: 1.5;
  color: #999;
}

/* =========================
   Modal buttons
========================= */

.modal-actions {
  display: grid;
  grid-template-columns: 1fr 1fr;
  gap: 8px;

  margin-top: 20px;
}

.modal-actions button {
  min-height: 48px;

  border: none;
  border-radius: 12px;

  font-size: 14px;
  font-weight: 700;

  cursor: pointer;
}

.secondary-btn {
  background: #f1f4f2;
  color: #555;
}

.cancel-btn {
  background: #f7f7f7;
  color: #777;
}

.save-btn {
  grid-column: 1 / -1;

  background: #4f9569;
  color: white;

  box-shadow: 0 5px 12px rgba(79, 149, 105, 0.22);
}

.save-btn:hover {
  background: #427f59;
}

.modal-actions button:active {
  transform: scale(0.98);
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

  width: min(calc(100% - 32px), 430px);

  display: flex;
  align-items: center;
  gap: 10px;

  padding: 14px 16px;

  border-radius: 14px;

  background: #262b28;
  color: white;

  font-size: 13px;
  line-height: 1.4;

  box-shadow: 0 10px 30px rgba(0, 0, 0, 0.2);
}

.toast-info {
  background: #365b47;
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

.toast-close {
  margin-left: auto;

  border: none;
  background: transparent;

  color: rgba(255, 255, 255, 0.7);

  font-size: 20px;
  cursor: pointer;
}

.toast-enter-active,
.toast-leave-active {
  transition: all 0.25s ease;
}

.toast-enter-from,
.toast-leave-to {
  opacity: 0;
  transform: translate(-50%, 15px);
}

/* =========================
   Mobile
========================= */

@media (max-width: 600px) {

  .calendar-page {
    padding: 12px 10px 32px;
  }

  .app-header {
    margin-bottom: 12px;
  }

  .goal-info {
    padding: 20px;
    border-radius: 18px;
  }

  .goal-period {
    gap: 10px;
  }

  .calendar-container {
    padding: 14px 10px 16px;
    border-radius: 18px;
  }

  .cal-grid {
    gap: 3px;
  }

  .day-box {
    border-radius: 9px;
    padding-top: 7px;
  }

  .mark-circle {
    width: 29px;
    height: 29px;
    font-size: 17px;
  }

  .day-badge {
    font-size: 6px;
  }

  .modal-overlay {
    align-items: flex-end;
    padding: 0;
  }

  .modal-content {
    width: 100%;
    max-height: 92vh;

    padding: 22px 18px;
    border-radius: 24px 24px 0 0;

    padding-bottom: calc(
      22px + env(safe-area-inset-bottom)
    );
  }

  .canvas-container {
    width: min(82vw, 300px);
  }

  .toast {
    bottom: calc(
      16px + env(safe-area-inset-bottom)
    );
  }
}

@media (max-width: 360px) {

  .calendar-container {
    padding-left: 7px;
    padding-right: 7px;
  }

  .cal-grid {
    gap: 2px;
  }

  .day-box {
    border-radius: 7px;
  }

  .mark-circle {
    width: 25px;
    height: 25px;
    font-size: 15px;
  }

  .day-badge {
    display: none;
  }
}

/* =========================
   Logout confirmation
========================= */

.confirm-overlay {
  position: fixed;
  inset: 0;
  z-index: 3000;

  display: flex;
  align-items: center;
  justify-content: center;

  padding: 20px;

  background: rgba(20, 27, 23, 0.55);
  backdrop-filter: blur(5px);
}

.confirm-modal {
  width: min(100%, 360px);

  padding: 28px 24px 24px;

  background: white;
  border-radius: 22px;

  text-align: center;

  box-shadow: 0 20px 60px rgba(0, 0, 0, 0.2);

  animation: modal-in 0.18s ease-out;
}

.confirm-icon {
  width: 52px;
  height: 52px;

  margin: 0 auto 16px;

  display: flex;
  align-items: center;
  justify-content: center;

  border-radius: 50%;

  background: #f1f4f2;
  color: #5f7467;

  font-size: 25px;
  font-weight: bold;
}

.confirm-modal h2 {
  margin: 0;

  font-size: 20px;
}

.confirm-modal p {
  margin: 10px 0 24px;

  font-size: 13px;
  line-height: 1.6;

  color: #777;
}

.confirm-actions {
  display: grid;
  grid-template-columns: 1fr 1fr;
  gap: 10px;
}

.confirm-actions button {
  min-height: 48px;

  border: none;
  border-radius: 12px;

  font-size: 14px;
  font-weight: 700;

  cursor: pointer;

  transition: 0.15s;
}

.confirm-actions button:active {
  transform: scale(0.97);
}

.confirm-cancel {
  background: #f1f3f2;
  color: #555;
}

.confirm-logout {
  background: #555;
  color: white;
}

.confirm-logout:hover {
  background: #333;
}

@keyframes modal-in {
  from {
    opacity: 0;
    transform: scale(0.95);
  }

  to {
    opacity: 1;
    transform: scale(1);
  }
}

@media (max-width: 600px) {

  .confirm-overlay {
    align-items: flex-end;
    padding: 0;
  }

  .confirm-modal {
    width: 100%;

    border-radius: 24px 24px 0 0;

    padding: 28px 20px;

    padding-bottom: calc(
      28px + env(safe-area-inset-bottom)
    );

    animation: modal-up 0.2s ease-out;
  }

}

@keyframes modal-up {
  from {
    opacity: 0;
    transform: translateY(30px);
  }

  to {
    opacity: 1;
    transform: translateY(0);
  }
}

</style>