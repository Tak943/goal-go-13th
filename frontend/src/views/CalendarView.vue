<script setup>
import { ref, onMounted, nextTick, computed } from 'vue';
import axios from 'axios';
import { useRoute } from 'vue-router';

const route = useRoute();
const goalId = route.params.goalId;
const goal = ref({});

const markedMap = ref({});

// カレンダーの表示用の年月（初期値は今日）
const viewYear = ref(new Date().getFullYear());
const viewMonth = ref(new Date().getMonth());

onMounted(async () => {
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
    alert("すでに記録済みです！");
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
    alert("✕になっていないようです!2本の線が交差するように描いてください。");
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
    alert("エラーが発生しました。");
    console.error(error);
  }
};
</script>

<template>
  <main>
    <h1>目標ID: {{ goal.id }} 目標タイトル: {{ goal.title }} のカレンダー画面</h1>
    <ul>
      <li>目標開始日: {{ goal.startDate }}</li>
      <li>目標日: {{ goal.targetDate }}</li>
    </ul>

    <!-- 【ここから変更】月ごとのカレンダー形式 -->
    <div class="calendar-container">
      
      <!-- カレンダー上部（月切り替え） -->
      <div class="cal-header">
        <button @click="prevMonth">＜ 先月</button>
        <h2>{{ viewYear }}年 {{ viewMonth + 1 }}月</h2>
        <button @click="nextMonth">来月 ＞</button>
      </div>
      
      <!-- 曜日 -->
      <div class="cal-weekdays">
        <span class="sun">日</span><span>月</span><span>火</span><span>水</span>
        <span>木</span><span>金</span><span class="sat">土</span>
      </div>
      
      <!-- カレンダーのマス目 -->
      <div class="cal-grid">
        <div 
          v-for="(day, index) in calendarGrid" 
          :key="day ? day.apiDate : index" 
          class="day-box" 
          :class="{ 
            'is-empty': !day, 
            'out-of-range': day && !day.isWithinRange,
            'marked': day && day.isMarked 
          }"
          @click="openModal(day)"
        >
          <!-- 日付の数字 -->
          <span v-if="day" class="date-text">{{ day.dayNum }}</span>
          
          <!-- 【追加】開始日の炎イラスト -->
          <span v-if="day && day.isStartDay" class="icon-start">🔥</span>

          <!-- 【追加】目標日の星イラスト -->
          <span v-if="day && day.isTargetDay" class="icon-target">⭐</span>
          
          <!-- スタンプ -->
          <span v-if="day && day.isMarked" class="stamp">❌</span>
        </div>
      </div>
      
    </div>
    
    <!-- おまけ：一覧に戻るリンク -->
    <RouterLink :to="{name: 'home'}">一覧に戻る</RouterLink>

    <!-- ※ お絵かきモーダルのコード（<div v-if="isModalOpen"... ）は全く同じままでOKです！ -->
    <div v-if="isModalOpen" class="modal-overlay" @click.self="closeModal">
      <!-- ... (省略: さっきまでのモーダルの中身そのまま) ... -->
      <div class="modal-content">
        <h2>{{ selectedDay.displayDate }} の記録</h2>
        <p>ここに「✕」を描いてください</p>
        <div class="canvas-container">
          <div class="guide-x">✕</div>
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
        <div class="modal-actions">
          <button @click="clearCanvas">描き直す</button>
          <button @click="closeModal">キャンセル</button>
          <button class="save-btn" @click="saveMark">登録する</button>
        </div>
      </div>
    </div>

  </main>
</template>

<!-- スタイル（デザイン）をここに書きます。「scoped」はこの画面だけに適用するという意味です -->
<style scoped>
/* --- カレンダー全体のレイアウト --- */
.calendar-container {
  max-width: 600px;
  /* margin: 20px 0; を以下に変更（auto をつけると左右の余白が均等になり、中央に配置されます！） */
  margin: 20px auto; 
  padding: 20px;
  background-color: white;
  border-radius: 12px;
  box-shadow: 0 4px 15px rgba(0,0,0,0.1);
}

.cal-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 20px;
}
.cal-header h2 { margin: 0; }
.cal-header button {
  padding: 8px 15px;
  cursor: pointer;
  border: 1px solid #ccc;
  border-radius: 6px;
  background-color: #f9f9f9;
}

.cal-weekdays {
  display: grid;
  grid-template-columns: repeat(7, 1fr);
  text-align: center;
  font-weight: bold;
  margin-bottom: 10px;
}
.sun { color: red; }
.sat { color: blue; }

/* 7列のマス目 */
.cal-grid {
  display: grid;
  grid-template-columns: repeat(7, 1fr);
  gap: 5px; /* マス目同士の隙間 */
}

/* 1つ1つのマス目のデザイン */
.day-box {
  aspect-ratio: 1 / 1; /* 高さを指定しなくても自動で正方形になる魔法 */
  border: 2px solid #e0e0e0;
  border-radius: 8px;
  display: flex;
  align-items: center;
  justify-content: center;
  font-weight: bold;
  font-size: 1.2rem;
  background-color: #f9f9f9;
  cursor: pointer;
  position: relative;
}

/* 通常の日付（期間内）にマウスを乗せた時 */
.day-box:not(.is-empty):not(.out-of-range):hover {
  background-color: #e0f7fa;
  border-color: #26c6da;
}

/* 空白マス（1日より前など） */
.day-box.is-empty {
  background-color: transparent;
  border: none;
  cursor: default;
}

/* 【追加】目標期間外のマス目（グレーアウト） */
.day-box.out-of-range {
  background-color: #f0f0f0;
  color: #ccc;
  border-color: #f0f0f0;
  cursor: not-allowed;
}

/* マーク済みのマス目 */
.day-box.marked {
  background-color: #ffebee;
  border-color: #ffcdd2;
  cursor: default; /* クリックできないように見せる */
}

/* 【追加】炎のイラスト（マス目の左上に配置） */
.icon-start {
  position: absolute;
  top: 2px;
  left: 4px;
  font-size: 16px;
  opacity: 0.9;
}

/* 【追加】星のイラスト（マス目の右下に配置） */
.icon-target {
  position: absolute;
  bottom: 0px;
  right: 4px;
  font-size: 18px;
}

.stamp {
  position: absolute;
  font-size: 35px;
  color: red;
  opacity: 0.7;
  z-index: 10; /* 【追加】スタンプが一番上にくるように念のため追加 */
}


/* --- ここからポップアップ（モーダル）用のデザイン --- */

/* 画面全体を覆う暗い半透明の背景 */
.modal-overlay {
  position: fixed;
  top: 0;
  left: 0;
  width: 100vw;
  height: 100vh;
  background-color: rgba(0, 0, 0, 0.5);
  display: flex;
  align-items: center;
  justify-content: center;
  z-index: 1000; /* 一番手前に表示する */
}

/* ポップアップの白い箱 */
.modal-content {
  background-color: white;
  padding: 30px;
  border-radius: 12px;
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 15px;
  box-shadow: 0 4px 15px rgba(0,0,0,0.2);
}

/* --- キャンバスと薄い✕を重ねるための設定 --- */
.canvas-container {
  position: relative;
  width: 300px;
  height: 300px;
}

/* 薄い✕のデザイン */
.guide-x {
  position: absolute;
  top: 50%;
  left: 50%;
  transform: translate(-50%, -50%);
  font-size: 250px;
  color: #e0e0e0; /* 薄いグレー */
  user-select: none; /* テキストとして選択されないようにする */
  z-index: 1; /* キャンバスの下に配置 */
}

/* キャンバスのデザイン（一部修正） */
.draw-canvas {
  position: absolute;
  top: 0;
  left: 0;
  border: 3px dashed #ccc;
  border-radius: 8px;
  background-color: transparent; /* 【重要】背景を透明にして下の✕を透かす */
  cursor: crosshair;
  touch-action: none;
  z-index: 2; /* ✕の上に配置 */
}

/* ボタンエリアのデザイン */
.modal-actions {
  display: flex;
  gap: 10px;
  width: 100%;
  justify-content: center;
  margin-top: 10px;
}

.modal-actions button {
  padding: 10px 16px;
  border-radius: 6px;
  border: 1px solid #ccc;
  cursor: pointer;
  font-weight: bold;
}

.save-btn {
  background-color: #ff5252;
  color: white;
  border: none !important;
}
.save-btn:hover {
  background-color: #ff1744;
}
</style>