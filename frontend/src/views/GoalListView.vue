<script setup>
import { ref, onMounted, computed } from 'vue';
import axios from 'axios';
import { useRouter } from 'vue-router';

//すべての通信の先頭に自動でくっつく
// axios.defaults.baseURL = 'http://localhost:8080';
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
// --- ログアウト用モーダル ---
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

// --- Toast ---
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

// --- 削除確認用モーダル ---
const isDeleteModalOpen = ref(false);
const deletingGoalId = ref(null);
const deletingGoalTitle = ref("");

const deleteGoal = (id) => {
  const goal = goals.value.find(goal => goal.id === id);

  deletingGoalId.value = id;
  deletingGoalTitle.value = goal ? goal.title : "";

  isDeleteModalOpen.value = true;
};

const cancelDelete = () => {
  isDeleteModalOpen.value = false;
  deletingGoalId.value = null;
  deletingGoalTitle.value = "";
};

const executeDelete = async () => {
  try {
    await axios.delete(`/api/goals/${deletingGoalId.value}`);

    await fetchGoals();

    cancelDelete();

    showToast("目標を削除しました。", "success");

  } catch (error) {
    showToast("削除に失敗しました。");
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
        showToast(
            "更新に失敗しました。開始日が目標日より後になっていないか確認してください。"
        );
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
  <main class="app-container">

    <!-- ==============================
         ヘッダー
    =============================== -->
    <header class="app-header">

      <div class="header-title">
        <span class="header-icon">🎯</span>
        <div>
          <h1>マイゴール</h1>
          <p>目標を一つずつ達成しよう</p>
        </div>
      </div>

      <button
        @click="logout"
        class="logout-btn"
      >
        ログアウト
      </button>

    </header>


    <!-- ==============================
         新しい目標を追加
    =============================== -->
    <section class="add-goal-card">

      <div class="section-heading">
        <div>
          <h2>新しい目標</h2>
          <p>達成したい目標を設定しましょう</p>
        </div>
      </div>

      <div class="goal-form">

        <!-- 目標タイトル -->
        <div class="form-group">
          <label>目標タイトル</label>

          <input
            type="text"
            v-model="newGoalTitle"
            placeholder="例：毎日30分英語を勉強する"
            class="form-input"
          >
        </div>


        <!-- 日付 -->
        <div class="date-row">

          <div class="form-group">
            <label>開始日</label>

            <button
              type="button"
              class="date-input"
              @click="openCalendar('start')"
            >
              <span class="calendar-icon">📅</span>

              <span
                :class="{ 'placeholder-text': !newGoalStartDate }"
              >
                {{ newGoalStartDate || '開始日を選択' }}
              </span>
            </button>
          </div>


          <div class="form-group">
            <label>目標日</label>

            <button
              type="button"
              class="date-input"
              @click="openCalendar('target')"
            >
              <span class="calendar-icon">📅</span>

              <span
                :class="{ 'placeholder-text': !newGoalTargetDate }"
              >
                {{ newGoalTargetDate || '目標日を選択' }}
              </span>
            </button>
          </div>

        </div>


        <!-- 追加ボタン -->
        <button
          @click="addGoal"
          class="add-btn"
        >
          <span>＋</span>
          目標を追加
        </button>

      </div>

    </section>


    <!-- ==============================
         目標一覧
    =============================== -->
    <section class="goals-section">

      <div class="section-heading goals-heading">

        <div>
          <h2>あなたの目標</h2>
          <p>目標をタップして達成状況を確認できます</p>
        </div>

        <span class="goal-count">
          {{ goals.length }}個
        </span>

      </div>


      <!-- 目標がない場合 -->
      <div
        v-if="goals.length === 0"
        class="empty-state"
      >
        <div class="empty-icon">🎯</div>

        <h3>まだ目標がありません</h3>

        <p>
          上のフォームから<br>
          最初の目標を追加してみましょう
        </p>
      </div>


      <!-- 目標一覧 -->
      <ul v-else class="goal-list">

        <li
          v-for="goal in goals"
          :key="goal.id"
          class="goal-item"
        >

          <!-- 目標 -->
          <RouterLink
            :to="{
              name: 'calendar',
              params: { goalId: goal.id }
            }"
            class="goal-link"
          >

            <div class="goal-main">

              <div class="goal-icon">
                🎯
              </div>

              <div class="goal-info">

                <span class="goal-title">
                  {{ goal.title }}
                </span>

                <span class="goal-date">
                  {{ goal.startDate }} ～ {{ goal.targetDate }}
                </span>

              </div>

            </div>

            <span class="arrow">
              ›
            </span>

          </RouterLink>


          <!-- 操作ボタン -->
          <div class="action-buttons">

            <button
              @click="openEditModal(goal)"
              class="edit-btn"
              aria-label="目標を修正"
            >
              修正
            </button>

            <button
              @click="deleteGoal(goal.id)"
              class="delete-btn"
              aria-label="目標を削除"
            >
              削除
            </button>

          </div>

        </li>

      </ul>

    </section>


    <!-- ==============================
         カレンダーモーダル
    =============================== -->
    <div
      v-if="isCalendarOpen"
      class="modal-overlay calendar-overlay"
      @click.self="closeCalendar"
    >

      <div class="calendar-modal">

        <!-- モバイル用ハンドル -->
        <div class="modal-handle"></div>

        <div class="modal-title-area">
          <h2>日付を選択</h2>
          <button
            @click="closeCalendar"
            class="modal-close-btn"
            aria-label="閉じる"
          >
            ×
          </button>
        </div>


        <!-- 月切り替え -->
        <div class="cal-header">

          <button
            class="cal-nav-btn"
            @click="prevMonth"
            aria-label="前の月"
          >
            ‹
          </button>

          <span class="cal-title">
            {{ viewYear }}年 {{ viewMonth + 1 }}月
          </span>

          <button
            class="cal-nav-btn"
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


    <!-- ==============================
         目標修正モーダル
    =============================== -->
    <div
      v-if="isEditModalOpen"
      class="modal-overlay edit-overlay"
      @click.self="closeEditModal"
    >

      <div class="edit-modal">

        <div class="modal-handle"></div>

        <div class="modal-title-area">

          <div>
            <h2>目標を修正</h2>
            <p>目標の内容を変更できます</p>
          </div>

          <button
            @click="closeEditModal"
            class="modal-close-btn"
            aria-label="閉じる"
          >
            ×
          </button>

        </div>


        <div class="edit-form">

          <!-- タイトル -->
          <div class="form-group">

            <label for="edit-title">
              目標タイトル
            </label>

            <input
              id="edit-title"
              type="text"
              v-model="editGoalTitle"
              placeholder="目標を入力してください"
              class="form-input"
            >

          </div>


          <!-- 開始日 -->
          <div class="form-group">

            <label>
              開始日
            </label>

            <button
              type="button"
              class="date-input"
              @click="openCalendar('editStart')"
            >

              <span class="calendar-icon">📅</span>

              <span>
                {{ editGoalStartDate }}
              </span>

            </button>

          </div>


          <!-- 目標日 -->
          <div class="form-group">

            <label>
              目標日
            </label>

            <button
              type="button"
              class="date-input"
              @click="openCalendar('editTarget')"
            >

              <span class="calendar-icon">📅</span>

              <span>
                {{ editGoalTargetDate }}
              </span>

            </button>

          </div>

        </div>


        <!-- ボタン -->
        <div class="modal-actions">

          <button
            @click="closeEditModal"
            class="cancel-btn"
          >
            キャンセル
          </button>

          <button
            @click="updateGoal"
            class="save-btn"
          >
            保存する
          </button>

        </div>

      </div>

    </div>

    <!-- ==============================
     ログアウト確認モーダル
============================== -->
<div
  v-if="isLogoutModalOpen"
  class="modal-overlay confirm-overlay"
  @click.self="cancelLogout"
>
  <div class="confirm-modal">

    <div class="confirm-icon logout-icon">
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
<!-- ==============================
     目標削除確認モーダル
============================== -->
<div
  v-if="isDeleteModalOpen"
  class="modal-overlay confirm-overlay"
  @click.self="cancelDelete"
>
  <div class="confirm-modal">

    <div class="confirm-icon delete-icon">
      !
    </div>

    <h2>目標を削除しますか？</h2>

    <p>
      「{{ deletingGoalTitle }}」を削除します。<br>
      記録したカレンダーの✕もすべて削除されます。
    </p>

    <div class="confirm-actions">

      <button
        class="confirm-cancel"
        @click="cancelDelete"
      >
        キャンセル
      </button>

      <button
        class="confirm-delete"
        @click="executeDelete"
      >
        削除する
      </button>

    </div>

  </div>
</div>
<!-- ==============================
     Toast
============================== -->
<Transition name="toast">
  <div
    v-if="toast.show"
    class="toast"
    :class="`toast-${toast.type}`"
  >
    <span class="toast-icon">
      {{ toast.type === 'success' ? '✓' : '!' }}
    </span>

    <span>{{ toast.message }}</span>
  </div>
</Transition>
  </main>
</template>


<style scoped>

/* ========================================
   基本設定
======================================== */

* {
  box-sizing: border-box;
}

button,
input {
  font-family: inherit;
}

button {
  -webkit-tap-highlight-color: transparent;
}

input {
  font-size: 16px;
}


/* ========================================
   メイン
======================================== */

.app-container {
  width: 100%;
  max-width: 760px;

  margin: 0 auto;

  padding: 32px 20px 60px;

  color: #1f2937;

  background: #f7f8fa;

  min-height: 100vh;
}


/* ========================================
   ヘッダー
======================================== */

.app-header {
  display: flex;

  align-items: center;
  justify-content: space-between;

  gap: 16px;

  margin-bottom: 28px;
}

.header-title {
  display: flex;

  align-items: center;

  gap: 12px;
}

.header-icon {
  width: 46px;
  height: 46px;

  display: flex;
  align-items: center;
  justify-content: center;

  border-radius: 14px;

  background: #e8f5e9;

  font-size: 22px;
}

.header-title h1 {
  margin: 0;

  font-size: 25px;
  font-weight: 800;

  letter-spacing: -0.5px;
}

.header-title p {
  margin: 3px 0 0;

  color: #8a9199;

  font-size: 12px;
}


/* ログアウト */

.logout-btn {
  min-height: 40px;

  padding: 8px 14px;

  border: 1px solid #e1e4e8;

  border-radius: 9px;

  background: white;

  color: #777;

  font-size: 12px;
  font-weight: 600;

  cursor: pointer;

  transition: 0.15s;
}

.logout-btn:hover {
  background: #f1f1f1;
}

.logout-btn:active {
  transform: scale(0.96);
}


/* ========================================
   セクション
======================================== */

.add-goal-card {
  padding: 22px;

  background: white;

  border: 1px solid #e9ecef;

  border-radius: 16px;

  box-shadow:
    0 3px 12px rgba(0, 0, 0, 0.04);

  margin-bottom: 32px;
}

.section-heading {
  display: flex;

  align-items: center;
  justify-content: space-between;

  margin-bottom: 20px;
}

.section-heading h2 {
  margin: 0;

  font-size: 18px;
  font-weight: 750;
}

.section-heading p {
  margin: 4px 0 0;

  color: #9aa0a6;

  font-size: 12px;
}


/* ========================================
   フォーム
======================================== */

.goal-form,
.edit-form {
  display: flex;

  flex-direction: column;

  gap: 15px;
}

.form-group {
  display: flex;

  flex-direction: column;

  gap: 7px;
}

.form-group label {
  color: #4b5563;

  font-size: 13px;
  font-weight: 700;
}


/* 通常入力 */

.form-input {
  width: 100%;

  min-height: 50px;

  padding: 13px 14px;

  border: 1px solid #dfe3e7;

  border-radius: 10px;

  background: #fafbfc;

  color: #222;

  font-size: 16px;

  outline: none;

  transition: 0.15s;
}

.form-input::placeholder {
  color: #b4bac0;
}

.form-input:focus {
  border-color: #4caf50;

  background: white;

  box-shadow:
    0 0 0 3px rgba(76, 175, 80, 0.1);
}


/* ========================================
   日付
======================================== */

.date-row {
  display: grid;

  grid-template-columns: 1fr 1fr;

  gap: 12px;
}

.date-input {
  width: 100%;

  min-height: 50px;

  display: flex;

  align-items: center;

  gap: 9px;

  padding: 12px 13px;

  border: 1px solid #dfe3e7;

  border-radius: 10px;

  background: #fafbfc;

  color: #333;

  font-size: 14px;

  text-align: left;

  cursor: pointer;

  transition: 0.15s;
}

.date-input:hover {
  border-color: #4caf50;
}

.date-input:active {
  transform: scale(0.98);
}

.calendar-icon {
  font-size: 16px;
}

.placeholder-text {
  color: #aeb4ba;
}


/* ========================================
   追加ボタン
======================================== */

.add-btn {
  width: 100%;

  min-height: 50px;

  display: flex;

  align-items: center;
  justify-content: center;

  gap: 6px;

  border: none;

  border-radius: 10px;

  background: #4caf50;

  color: white;

  font-size: 15px;
  font-weight: 750;

  cursor: pointer;

  box-shadow:
    0 4px 10px rgba(76, 175, 80, 0.2);

  transition: 0.15s;
}

.add-btn span {
  font-size: 20px;
}

.add-btn:hover {
  background: #43a047;
}

.add-btn:active {
  transform: translateY(1px) scale(0.99);
}


/* ========================================
   目標一覧
======================================== */

.goals-section {
  width: 100%;
}

.goals-heading {
  margin-bottom: 14px;
}

.goal-count {
  padding: 5px 10px;

  border-radius: 20px;

  background: #e8f5e9;

  color: #43a047;

  font-size: 12px;
  font-weight: 700;
}


/* ========================================
   目標カード
======================================== */

.goal-list {
  display: flex;

  flex-direction: column;

  gap: 10px;

  list-style: none;

  padding: 0;
  margin: 0;
}

.goal-item {
  display: flex;

  align-items: center;

  gap: 12px;

  padding: 13px 14px;

  background: white;

  border: 1px solid #e9ecef;

  border-radius: 13px;

  box-shadow:
    0 2px 8px rgba(0, 0, 0, 0.03);

  transition: 0.15s;
}

.goal-item:hover {
  border-color: #d5ddd7;

  box-shadow:
    0 4px 12px rgba(0, 0, 0, 0.06);
}


/* 目標リンク */

.goal-link {
  min-width: 0;

  flex: 1;

  display: flex;

  align-items: center;

  gap: 11px;

  color: inherit;

  text-decoration: none;
}

.goal-main {
  min-width: 0;

  flex: 1;

  display: flex;

  align-items: center;

  gap: 11px;
}


/* チェックアイコン */

.goal-icon {
  width: 38px;
  height: 38px;

  flex-shrink: 0;

  display: flex;
  align-items: center;
  justify-content: center;

  border-radius: 10px;

  background: #e8f5e9;

  color: #4caf50;

  font-size: 17px;
  font-weight: bold;
}


/* 目標情報 */

.goal-info {
  min-width: 0;

  display: flex;

  flex-direction: column;

  gap: 4px;
}

.goal-title {
  overflow: hidden;

  color: #2f3437;

  font-size: 15px;
  font-weight: 700;

  text-overflow: ellipsis;

  white-space: nowrap;
}

.goal-date {
  color: #a0a6ac;

  font-size: 11px;
}


/* 矢印 */

.arrow {
  flex-shrink: 0;

  color: #b5bbc0;

  font-size: 25px;

  line-height: 1;
}


/* ========================================
   編集・削除
======================================== */

.action-buttons {
  display: flex;

  flex-shrink: 0;

  gap: 6px;
}

.edit-btn,
.delete-btn {
  min-width: 52px;

  min-height: 40px;

  padding: 7px 10px;

  border: none;

  border-radius: 8px;

  font-size: 12px;
  font-weight: 700;

  cursor: pointer;

  transition: 0.15s;
}

.edit-btn {
  background: #eef7ef;

  color: #43a047;
}

.delete-btn {
  background: #fff0f0;

  color: #e53935;
}

.edit-btn:hover {
  background: #dff1e1;
}

.delete-btn:hover {
  background: #ffe1e1;
}

.edit-btn:active,
.delete-btn:active {
  transform: scale(0.94);
}


/* ========================================
   空の状態
======================================== */

.empty-state {
  padding: 50px 20px;

  text-align: center;

  background: white;

  border: 1px dashed #dfe3e7;

  border-radius: 14px;
}

.empty-icon {
  margin-bottom: 12px;

  font-size: 40px;
}

.empty-state h3 {
  margin: 0 0 7px;

  font-size: 16px;
}

.empty-state p {
  margin: 0;

  color: #9aa0a6;

  font-size: 13px;

  line-height: 1.7;
}


/* ========================================
   モーダル
======================================== */

.modal-overlay {
  position: fixed;

  inset: 0;

  width: 100%;
  height: 100dvh;

  display: flex;

  align-items: center;
  justify-content: center;

  padding: 20px;

  background: rgba(20, 25, 22, 0.55);

  backdrop-filter: blur(3px);

  z-index: 1000;

  overflow-y: auto;
}


/* モーダル本体 */

.calendar-modal,
.edit-modal {
  width: min(100%, 430px);

  padding: 22px;

  background: white;

  border-radius: 18px;

  box-shadow:
    0 20px 50px rgba(0, 0, 0, 0.18);
}


/* モーダルタイトル */

.modal-title-area {
  display: flex;

  align-items: center;
  justify-content: space-between;

  margin-bottom: 20px;
}

.modal-title-area h2 {
  margin: 0;

  font-size: 19px;
  font-weight: 800;
}

.modal-title-area p {
  margin: 4px 0 0;

  color: #9aa0a6;

  font-size: 12px;
}

.modal-close-btn {
  width: 38px;
  height: 38px;

  display: flex;
  align-items: center;
  justify-content: center;

  border: none;

  border-radius: 50%;

  background: #f3f4f4;

  color: #777;

  font-size: 24px;

  cursor: pointer;
}

.modal-close-btn:active {
  transform: scale(0.92);
}


/* スマホでのみ表示するハンドル */

.modal-handle {
  display: none;
}


/* ========================================
   カレンダー
======================================== */

.cal-header {
  display: flex;

  align-items: center;
  justify-content: space-between;

  margin-bottom: 15px;
}

.cal-title {
  font-size: 18px;
  font-weight: 800;
}

.cal-nav-btn {
  width: 44px;
  height: 44px;

  display: flex;
  align-items: center;
  justify-content: center;

  border: 1px solid #e1e5e8;

  border-radius: 10px;

  background: #f8f9fa;

  color: #555;

  font-size: 28px;

  line-height: 1;

  cursor: pointer;

  transition: 0.15s;
}

.cal-nav-btn:hover {
  background: #e8f5e9;

  color: #43a047;
}

.cal-nav-btn:active {
  transform: scale(0.92);
}


/* 曜日 */

.cal-weekdays {
  display: grid;

  grid-template-columns: repeat(7, 1fr);

  margin-bottom: 5px;

  text-align: center;

  font-size: 12px;
  font-weight: 700;
}

.cal-weekdays span {
  padding: 5px 0;
}

.sun {
  color: #e53935;
}

.sat {
  color: #1e88e5;
}


/* 日付 */

.cal-grid {
  display: grid;

  grid-template-columns: repeat(7, 1fr);

  gap: 4px;
}

.cal-cell {
  aspect-ratio: 1 / 1;

  min-height: 43px;

  display: flex;
  align-items: center;
  justify-content: center;

  border-radius: 9px;

  font-size: 14px;
}

.cal-cell.is-day {
  background: #f4f6f5;

  color: #333;

  cursor: pointer;

  transition: 0.12s;
}

.cal-cell.is-day:hover {
  background: #4caf50;

  color: white;

  font-weight: 700;
}

.cal-cell.is-day:active {
  transform: scale(0.9);
}

.cal-cell.is-empty {
  background: transparent;
}

.cal-cell.is-disabled {
  background: #f7f7f7;

  color: #d0d3d5;

  cursor: not-allowed;
}


/* ========================================
   編集フォーム
======================================== */

.edit-form {
  margin-bottom: 22px;
}


/* ========================================
   モーダルボタン
======================================== */

.modal-actions {
  display: grid;

  grid-template-columns: 1fr 1fr;

  gap: 9px;
}

.cancel-btn,
.save-btn {
  min-height: 48px;

  border-radius: 9px;

  font-size: 14px;
  font-weight: 700;

  cursor: pointer;

  transition: 0.15s;
}

.cancel-btn {
  border: 1px solid #dfe3e7;

  background: #f7f8f9;

  color: #555;
}

.save-btn {
  border: none;

  background: #2196f3;

  color: white;

  box-shadow:
    0 4px 10px rgba(33, 150, 243, 0.18);
}

.cancel-btn:active,
.save-btn:active {
  transform: scale(0.97);
}


/* ========================================
   スマートフォン
======================================== */

@media (max-width: 600px) {

  .app-container {
    padding:
      20px
      14px
      calc(40px + env(safe-area-inset-bottom));
  }


  /* ヘッダー */

  .app-header {
    margin-bottom: 20px;
  }

  .header-icon {
    width: 42px;
    height: 42px;

    border-radius: 12px;

    font-size: 19px;
  }

  .header-title h1 {
    font-size: 21px;
  }

  .header-title p {
    font-size: 11px;
  }

  .logout-btn {
    min-height: 42px;

    padding: 7px 11px;

    font-size: 11px;
  }


  /* 追加カード */

  .add-goal-card {
    padding: 18px 15px;

    margin-bottom: 26px;

    border-radius: 14px;
  }

  .section-heading h2 {
    font-size: 17px;
  }


  /* 日付 */

  .date-row {
    grid-template-columns: 1fr;
  }

  .date-input {
    min-height: 50px;

    font-size: 15px;
  }


  /* 目標 */

  .goal-item {
    flex-wrap: wrap;

    padding: 12px;
  }

  .goal-link {
    width: 100%;
  }

  .goal-icon {
    width: 36px;
    height: 36px;
  }

  .goal-title {
    font-size: 14px;
  }

  .action-buttons {
    width: 100%;
  }

  .edit-btn,
  .delete-btn {
    flex: 1;

    min-height: 44px;

    font-size: 13px;
  }


  /* モーダル */

  .modal-overlay {
    align-items: flex-end;

    padding: 0;
  }

  .calendar-modal,
  .edit-modal {
    width: 100%;

    max-width: none;

    padding:
      12px
      16px
      calc(20px + env(safe-area-inset-bottom));

    border-radius: 20px 20px 0 0;
  }

  .modal-handle {
    display: block;

    width: 38px;
    height: 4px;

    margin: 0 auto 16px;

    border-radius: 10px;

    background: #d8dadd;
  }

  .modal-title-area {
    margin-bottom: 16px;
  }


  /* カレンダー */

  .cal-header {
    margin-bottom: 12px;
  }

  .cal-title {
    font-size: 18px;
  }

  .cal-nav-btn {
    width: 46px;
    height: 46px;
  }

  .cal-grid {
    gap: 3px;
  }

  .cal-cell {
    min-height: 44px;

    border-radius: 9px;

    font-size: 14px;
  }


  /* 編集フォーム */

  .edit-modal {
    max-height: 90dvh;

    overflow-y: auto;
  }

  .edit-form {
    gap: 14px;
  }

  .form-input {
    min-height: 50px;
  }

  .modal-actions {
    gap: 8px;
  }

  .cancel-btn,
  .save-btn {
    min-height: 50px;
  }
}


/* ========================================
   小さいスマートフォン
======================================== */

@media (max-width: 360px) {

  .app-container {
    padding-left: 10px;
    padding-right: 10px;
  }

  .header-title h1 {
    font-size: 19px;
  }

  .logout-btn {
    padding: 7px 9px;
  }

  .add-goal-card {
    padding: 16px 12px;
  }

  .cal-cell {
    min-height: 40px;

    font-size: 13px;
  }

  .cal-grid {
    gap: 2px;
  }
}

/* ========================================
   確認モーダル
======================================== */

.confirm-overlay {
  z-index: 1100;
}

.confirm-modal {
  width: min(100%, 380px);
  padding: 28px 24px 24px;

  background: white;
  border-radius: 20px;

  text-align: center;

  box-shadow:
    0 20px 60px rgba(0, 0, 0, 0.2);
}

.confirm-icon {
  width: 52px;
  height: 52px;

  margin: 0 auto 16px;

  display: flex;
  align-items: center;
  justify-content: center;

  border-radius: 50%;

  font-size: 25px;
  font-weight: 800;
}

.logout-icon {
  background: #e8f5e9;
  color: #43a047;
}

.delete-icon {
  background: #fff0f0;
  color: #e53935;
}

.confirm-modal h2 {
  margin: 0 0 8px;

  color: #222;

  font-size: 18px;
  font-weight: 800;
}

.confirm-modal p {
  margin: 0;

  color: #8a9199;

  font-size: 13px;
  line-height: 1.7;
}

.confirm-actions {
  display: grid;
  grid-template-columns: 1fr 1fr;

  gap: 9px;

  margin-top: 24px;
}

.confirm-actions button {
  min-height: 48px;

  border-radius: 10px;

  font-size: 14px;
  font-weight: 700;

  cursor: pointer;

  transition: 0.15s;
}

.confirm-cancel {
  border: 1px solid #dfe3e7;

  background: #f7f8f9;
  color: #555;
}

.confirm-logout {
  border: none;

  background: #4caf50;
  color: white;
}

.confirm-delete {
  border: none;

  background: #e53935;
  color: white;
}

.confirm-actions button:active {
  transform: scale(0.96);
}


/* ========================================
   Toast
======================================== */

.toast {
  position: fixed;

  left: 50%;
  bottom: 24px;

  z-index: 2000;

  transform: translateX(-50%);

  display: flex;
  align-items: center;

  gap: 10px;

  width: max-content;
  max-width: calc(100vw - 32px);

  padding: 12px 16px;

  border-radius: 12px;

  background: #2f3437;
  color: white;

  font-size: 13px;
  font-weight: 600;

  box-shadow:
    0 8px 25px rgba(0, 0, 0, 0.18);
}

.toast-icon {
  width: 22px;
  height: 22px;

  flex-shrink: 0;

  display: flex;
  align-items: center;
  justify-content: center;

  border-radius: 50%;

  background: rgba(255, 255, 255, 0.15);

  font-size: 12px;
  font-weight: 800;
}

.toast-success .toast-icon {
  background: #4caf50;
}

.toast-error .toast-icon {
  background: #e53935;
}


/* Toast animation */

.toast-enter-active,
.toast-leave-active {
  transition:
    opacity 0.2s ease,
    transform 0.2s ease;
}

.toast-enter-from,
.toast-leave-to {
  opacity: 0;

  transform:
    translateX(-50%)
    translateY(10px);
}


/* ========================================
   スマートフォン
======================================== */

@media (max-width: 600px) {

  .confirm-overlay {
    align-items: flex-end;
    padding: 0;
  }

  .confirm-modal {
    width: 100%;
    max-width: none;

    padding:
      24px
      20px
      calc(20px + env(safe-area-inset-bottom));

    border-radius: 20px 20px 0 0;
  }

  .confirm-actions button {
    min-height: 52px;
  }

  .toast {
    bottom: calc(20px + env(safe-area-inset-bottom));

    max-width: calc(100vw - 24px);

    padding: 13px 15px;
  }
}
/* 通常のモーダル */
.modal-overlay {
  z-index: 1000;
}

/* 編集画面から開く日付カレンダー */
.calendar-overlay {
  z-index: 1200;
}

/* ログアウト・削除確認 */
.confirm-overlay {
  z-index: 1300;
}
</style>
