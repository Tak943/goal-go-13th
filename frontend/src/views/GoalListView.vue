<script setup>
import { ref, onMounted, computed } from 'vue';
import axios from 'axios';
import { useRouter } from 'vue-router';

//すべての通信の先頭に自動でくっつく
axios.defaults.baseURL = 'http://localhost:8080';
const router = useRouter();

//取得した目標リストの箱
const goals = ref([]);

const newGoalTitle = ref("");
const newGoalTargetDate = ref("");
const newGoalStartDate = ref("");

// --- 【追加】目標一覧を取得する共通関数 ---
const fetchGoals = async () => {
  const userId = localStorage.getItem('app_userId');
  if (!userId) return; // userIdが無ければ何もしない

  try {
    // 自分のuserIdをつけてGETリクエストを送る
    const response = await axios.get(`/api/goals?userId=${userId}`);
    goals.value = response.data;
  } catch (error) {
    console.error("目標の取得に失敗しました", error);
  }
};

// --- 【追加】ログアウト関数 ---
const logout = () => {
  if (confirm("ログアウトしますか？")) {
    localStorage.removeItem('app_token');
    localStorage.removeItem('app_userId');
    localStorage.removeItem('app_username');
    delete axios.defaults.headers.common['Authorization'];
    router.push({ name: 'login' });
  }
};

//追加ボタン押されたらPOST
const addGoal = async () => {
  await axios.post("/api/goals", {
    //送信するJSONの中身を指定。
    "title": newGoalTitle.value,
    "targetDate": newGoalTargetDate.value,
    "startDate": newGoalStartDate.value
  });

  // 送信し終わったら、newGoalをリセット。
  newGoalTitle.value = "";
  newGoalTargetDate.value = "";
  newGoalStartDate.value = "";

  //もう一度GETリクエストで取得しなおして、画面の目標一覧を更新。
  await fetchGoals();
}

const deleteGoal = async (id) => {
  // いきなり消えると危険なので、ブラウザ標準の確認メッセージ（ポップアップ）を出す
  const isConfirmed = confirm("本当に削除しますか？（記録したカレンダーの✕印もすべて消えます）");
  
  if (!isConfirmed) {
    return; // 「キャンセル」が押されたら何もしないで終了
  }

  try {
    // 削除のAPI（DELETEメソッド）を呼び出す
    await axios.delete(`/api/goals/${id}`);
    
    // 削除が成功したら、もう一度GETリクエストで取得しなおして画面を更新
    await fetchGoals();
    
  } catch (error) {
    alert("削除に失敗しました。");
    console.error(error);
  }
};

// --- 【追加】編集（修正）用の変数 ---
const isEditModalOpen = ref(false);
const editingGoalId = ref(null);
const editGoalTitle = ref("");
const editGoalStartDate = ref("");
const editGoalTargetDate = ref("");

// --- 【追加】編集ポップアップを開く・閉じる ---
const openEditModal = (goal) => {
  editingGoalId.value = goal.id;
  editGoalTitle.value = goal.title;
  editGoalStartDate.value = goal.startDate;
  editGoalTargetDate.value = goal.targetDate;
  isEditModalOpen.value = true;
};

const closeEditModal = () => {
  isEditModalOpen.value = false;
  editingGoalId.value = null;
};

// --- 【追加】修正データを送信する（PUTリクエスト） ---
const updateGoal = async () => {
  try {
    await axios.put(`/api/goals/${editingGoalId.value}`, {
      title: editGoalTitle.value,
      startDate: editGoalStartDate.value,
      targetDate: editGoalTargetDate.value
    });
    
    closeEditModal(); // 成功したら閉じる
    
    // 一覧を再取得して画面を更新
    await fetchGoals();
    
  } catch (error) {
    // 【重要】Java側で投げた 400 Bad Request をここでキャッチします！
    alert("更新に失敗しました。開始日が目標日より後になっていないか確認してください。");
    console.error(error);
  }
};

//画面が開いた瞬間に処理
//asyncは次に、データを取ってきたりする待ち時間のいる作業が発生しますよ。を宣言する目印
//awaitは実際の通信の前に置いて、この処理はデータが来るまでは待機してね。と指示する目印
onMounted(async () => {
  await fetchGoals();
})


// ポップアップを開いているかどうか
const isCalendarOpen = ref(false);
// どっちの入力欄（開始日？目標日？）をクリックしたかを記憶する箱
const activeField = ref(""); 

// カレンダーに表示している「年」と「月」（初期値は今日の年月）
const viewYear = ref(new Date().getFullYear());
const viewMonth = ref(new Date().getMonth()); // ※JSの月は0〜11で管理されます

// カレンダーを開く関数
const openCalendar = (field) => {
  activeField.value = field;
  isCalendarOpen.value = true;
};

// カレンダーを閉じる関数
const closeCalendar = () => {
  isCalendarOpen.value = false;
};

// 月を切り替える関数
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

// 【重要】カレンダーのマス目を計算する自動計算ツール
const calendarGrid = computed(() => {
  // ① その月の「1日」が何曜日かを取得（0:日曜 〜 6:土曜）
  const firstDayOfWeek = new Date(viewYear.value, viewMonth.value, 1).getDay();
  // ② その月の「最後の日（何日まであるか）」を取得する裏ワザ（翌月の「0日目」を指定すると当月の末日になる）
  const lastDate = new Date(viewYear.value, viewMonth.value + 1, 0).getDate();
  
  const days = [];
  
  // 1日より前の「空っぽのマス」を埋める
  for (let i = 0; i < firstDayOfWeek; i++) {
    days.push(null);
  }
  
  // 1日から月末までの数字を入れる
  for (let i = 1; i <= lastDate; i++) {
    days.push(i);
  }
  
  return days;
});

// --- 【追加】マス目の日付を YYYY-MM-DD 形式にする便利関数 ---
const getFormattedDate = (day) => {
  const m = String(viewMonth.value + 1).padStart(2, '0');
  const d = String(day).padStart(2, '0');
  return `${viewYear.value}-${m}-${d}`;
};

// --- 【追加】その日付がグレーアウト（選択不可）かどうかを判定する関数 ---
// --- 【変更】自作カレンダーを「編集画面」でも使えるようにする ---
const isDisabled = (day) => {
  if (!day) return false;
  const cellDate = getFormattedDate(day);

  // （既存）新規追加用の制限
  if (activeField.value === 'start' && newGoalTargetDate.value) return cellDate > newGoalTargetDate.value;
  if (activeField.value === 'target' && newGoalStartDate.value) return cellDate < newGoalStartDate.value;

  // 【追加】編集画面用の制限
  if (activeField.value === 'editStart' && editGoalTargetDate.value) return cellDate > editGoalTargetDate.value;
  if (activeField.value === 'editTarget' && editGoalStartDate.value) return cellDate < editGoalStartDate.value;

  return false;
};

// 日付をクリックして選択した時の処理
const selectDate = (day) => {
  if (!day || isDisabled(day)) return; 
  const formattedDate = getFormattedDate(day);
  
  // （既存）新規追加用
  if (activeField.value === 'start') newGoalStartDate.value = formattedDate;
  else if (activeField.value === 'target') newGoalTargetDate.value = formattedDate;
  
  // 【追加】編集画面用
  else if (activeField.value === 'editStart') editGoalStartDate.value = formattedDate;
  else if (activeField.value === 'editTarget') editGoalTargetDate.value = formattedDate;
  
  closeCalendar();
};
</script>

<template>
  <main>
    <div class="app-header">
      <h1>マイゴール一覧</h1>
      <button @click="logout" class="logout-btn">ログアウト</button>
    </div>

    <!-- 新規追加フォーム -->
    <div>
      <input type="text" v-model="newGoalTitle" placeholder="新しい目標を入力">
      
      <!-- 【変更】readonlyをつけてキーボードが出ないようにし、クリックで関数を呼ぶ -->
      <input 
        type="text" 
        :value="newGoalStartDate" 
        @click="openCalendar('start')" 
        readonly 
        placeholder="開始日を選択"
      >
      <input 
        type="text" 
        :value="newGoalTargetDate" 
        @click="openCalendar('target')" 
        readonly 
        placeholder="目標日を選択"
      >
      
      <button @click="addGoal">追加</button>
    </div>
    
    <ul>
      <!-- 【変更】class="goal-item" を追加 -->
      <li v-for="goal in goals" :key="goal.id" class="goal-item">
        
        <!-- 【変更】class="goal-link" を追加 -->
        <RouterLink :to="{ name: 'calendar', params: { goalId: goal.id }}" class="goal-link">
          {{ goal.title }}
        </RouterLink>
        
        <!-- 【変更】修正ボタンと削除ボタンを横に並べる箱を作る -->
        <div class="action-buttons">
          <button @click="openEditModal(goal)" class="edit-btn">修正</button>
          <button @click="deleteGoal(goal.id)" class="delete-btn">削除</button>
        </div>
      </li>
    </ul>

    <!-- 【ここから追加】カレンダーのポップアップ（モーダル） -->
    <div v-if="isCalendarOpen" class="modal-overlay" style="z-index: 1010;" @click.self="closeCalendar">
      <div class="calendar-modal">
        
        <!-- カレンダーの頭（年月と左右ボタン） -->
        <div class="cal-header">
          <button @click="prevMonth">＜</button>
          <span>{{ viewYear }}年 {{ viewMonth + 1 }}月</span>
          <button @click="nextMonth">＞</button>
        </div>
        
        <!-- 曜日 -->
        <div class="cal-weekdays">
          <span class="sun">日</span><span>月</span><span>火</span><span>水</span>
          <span>木</span><span>金</span><span class="sat">土</span>
        </div>
        
        <!-- 日付のマス目 -->
        <div class="cal-grid">
          <!-- dayがnullの時は数字が出ない -->
          <!-- 【変更】is-disabled クラスの条件を追加 -->
          <div 
            v-for="(day, index) in calendarGrid" 
            :key="index"
            class="cal-cell"
            :class="{ 
              'is-empty': !day, 
              'is-day': day && !isDisabled(day), 
              'is-disabled': isDisabled(day) 
            }"
            @click="selectDate(day)"
          >
            {{ day || '' }}
          </div>
        </div>
        
      </div>
    </div>

    <!-- 【ここから追加】目標修正のポップアップ（モーダル） -->
    <div v-if="isEditModalOpen" class="modal-overlay" style="z-index: 1000;" @click.self="closeEditModal">
      <div class="edit-modal">
        <h2>目標の修正</h2>
        
        <div class="edit-form">
          <label>目標タイトル</label>
          <input type="text" v-model="editGoalTitle">
          
          <label>開始日</label>
          <input type="text" :value="editGoalStartDate" @click="openCalendar('editStart')" readonly>
          
          <label>目標日</label>
          <input type="text" :value="editGoalTargetDate" @click="openCalendar('editTarget')" readonly>
        </div>
        
        <div class="modal-actions">
          <button @click="closeEditModal" class="cancel-btn">キャンセル</button>
          <button @click="updateGoal" class="save-btn">保存する</button>
        </div>
      </div>
    </div>
  </main>
</template>

<style scoped>
/* --- カレンダーモーダル用デザイン --- */
.modal-overlay {
  position: fixed;
  top: 0; left: 0;
  width: 100vw; height: 100vh;
  background-color: rgba(0, 0, 0, 0.5);
  display: flex;
  align-items: center; justify-content: center;
  z-index: 1000;
}

.calendar-modal {
  background-color: white;
  padding: 25px;
  border-radius: 12px;
  width: 90vw;
  max-width: 420px;
  box-shadow: 0 4px 15px rgba(0,0,0,0.2);
}

.cal-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  font-size: 20px;
  font-weight: bold;
  margin-bottom: 20px;
}
.cal-header button {
  padding: 8px 20px;
  cursor: pointer;
  border: 1px solid #ccc;
  border-radius: 4px;
  background-color: #f9f9f9;
  font-size: 16px;
}

.cal-weekdays {
  display: grid;
  grid-template-columns: repeat(7, 1fr);
  text-align: center;
  font-weight: bold;
  font-size: 16px;
  margin-bottom: 10px;
}
.sun { color: red; }
.sat { color: blue; }

/* マス目を横7列に自動で並べる設定 */
.cal-grid {
  display: grid;
  grid-template-columns: repeat(7, 1fr);
  gap: 5px;
}

.cal-cell {
  height: 45px;
  display: flex;
  align-items: center;
  justify-content: center;
  border-radius: 4px;
  font-size: 14px;
}

.cal-cell.is-day {
  cursor: pointer;
  background-color: #f0f0f0;
}
.cal-cell.is-day:hover {
  background-color: #4CAF50;
  color: white;
  font-weight: bold;
}
.cal-cell.is-empty {
  background-color: transparent;
  cursor: default;
}
/* --- 【追加】グレーアウト（選択不可）のマス目のデザイン --- */
.cal-cell.is-disabled {
  background-color: #f5f5f5;
  color: #ccc; /* 文字を薄いグレーに */
  cursor: not-allowed; /* マウスカーソルを「禁止マーク」にする */
}

/* グレーアウトのマスはマウスを乗せても色を変えない */
.cal-cell.is-disabled:hover {
  background-color: #f5f5f5;
  color: #ccc;
  font-weight: normal;
}

/* --- 【追加】目標リストと削除ボタンのデザイン --- */
ul {
  list-style: none; /* デフォルトの・（黒丸）を消す */
  padding: 0;
}

.goal-item {
  display: flex;
  justify-content: space-between; /* タイトルは左、ボタンは右に分ける */
  align-items: center;
  padding: 12px 16px;
  margin-bottom: 10px;
  background-color: #f9f9f9;
  border: 1px solid #eee;
  border-radius: 8px;
}

.goal-link {
  text-decoration: none;
  color: #333;
  font-weight: bold;
  font-size: 18px;
  flex-grow: 1; /* リンク部分を横いっぱいに広げて押しやすくする */
}
.goal-link:hover {
  color: #4CAF50; /* マウスを乗せたら緑色にする */
}

.delete-btn {
  background-color: #ff5252;
  color: white;
  border: none;
  border-radius: 6px;
  padding: 8px 12px;
  font-weight: bold;
  cursor: pointer;
}
.delete-btn:hover {
  background-color: #ff1744;
}
/* --- 【追加】修正ボタンと編集ポップアップのデザイン --- */
.action-buttons {
  display: flex;
  gap: 8px; /* ボタン同士の隙間 */
}

.edit-btn {
  background-color: #4CAF50;
  color: white;
  border: none;
  border-radius: 6px;
  padding: 8px 12px;
  font-weight: bold;
  cursor: pointer;
}
.edit-btn:hover {
  background-color: #45a049;
}

/* 編集モーダル本体 */
.edit-modal {
  background-color: white;
  padding: 25px;
  border-radius: 12px;
  width: 90vw;
  max-width: 400px;
  box-shadow: 0 4px 15px rgba(0,0,0,0.2);
}

.edit-modal h2 {
  margin-top: 0;
  text-align: center;
}

.edit-form {
  display: flex;
  flex-direction: column;
  gap: 10px;
  margin-bottom: 20px;
}

.edit-form label {
  font-size: 14px;
  font-weight: bold;
  color: #555;
  margin-bottom: -5px;
}

.edit-form input {
  padding: 10px;
  border: 1px solid #ccc;
  border-radius: 6px;
  font-size: 16px;
}

.modal-actions {
  display: flex;
  justify-content: center;
  gap: 15px;
}

.cancel-btn {
  padding: 10px 20px;
  border: 1px solid #ccc;
  background-color: #f9f9f9;
  border-radius: 6px;
  cursor: pointer;
  font-weight: bold;
}
.save-btn {
  padding: 10px 20px;
  background-color: #2196F3;
  color: white;
  border: none;
  border-radius: 6px;
  cursor: pointer;
  font-weight: bold;
}
.save-btn:hover {
  background-color: #0b7dda;
}
/* --- 【追加】ヘッダーとログアウトボタン --- */
.app-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 20px;
}

.logout-btn {
  padding: 8px 16px;
  background-color: #757575;
  color: white;
  border: none;
  border-radius: 6px;
  cursor: pointer;
  font-weight: bold;
}
.logout-btn:hover {
  background-color: #616161;
}
</style>
