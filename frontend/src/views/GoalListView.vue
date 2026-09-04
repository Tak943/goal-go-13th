<script setup>
import { ref, onMounted, computed } from 'vue';
import axios from 'axios';

//すべての通信の先頭に自動でくっつく
axios.defaults.baseURL = 'http://localhost:8080';

//取得した目標リストの箱
const goals = ref([]);

const newGoalTitle = ref("");
const newGoalTargetDate = ref("");
const newGoalStartDate = ref("");

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
  const response = await axios.get("/api/goals");
  goals.value = response.data;
}

//画面が開いた瞬間に処理
//asyncは次に、データを取ってきたりする待ち時間のいる作業が発生しますよ。を宣言する目印
//awaitは実際の通信の前に置いて、この処理はデータが来るまでは待機してね。と指示する目印
onMounted(async () => {
  // Spring BootのAPIからデータを取得する（GETリクエスト）
  const response = await axios.get("/api/goals");

  // axiosで通信した場合、実際のデータは response の中の `data` というプロパティに入っている
  goals.value = response.data;

  console.log(goals.value);
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
const isDisabled = (day) => {
  if (!day) return false; // 空っぽのマスは関係なし
  
  const cellDate = getFormattedDate(day);

  // ① もし「開始日」を選ぼうとしていて、すでに「目標日」が入力されているなら
  if (activeField.value === 'start' && newGoalTargetDate.value) {
    // 目標日より「未来」の日付は選べなくする（グレーアウト）
    return cellDate > newGoalTargetDate.value;
  }
  
  // ② もし「目標日」を選ぼうとしていて、すでに「開始日」が入力されているなら
  if (activeField.value === 'target' && newGoalStartDate.value) {
    // 開始日より「過去」の日付は選べなくする（グレーアウト）
    return cellDate < newGoalStartDate.value;
  }

  return false; // どちらの制限にも引っかからなければ、選んでOK！
};

// 日付をクリックして選択した時の処理
const selectDate = (day) => {
  // 空っぽのマス、または「グレーアウト（isDisabled）」のマスなら何もしない
  if (!day || isDisabled(day)) return; 
  
  const formattedDate = getFormattedDate(day);
  
  if (activeField.value === 'start') {
    newGoalStartDate.value = formattedDate;
  } else if (activeField.value === 'target') {
    newGoalTargetDate.value = formattedDate;
  }
  
  closeCalendar();
};
</script>

<template>
  <main>
    <h1>マイゴール一覧</h1>

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
      <!-- goals配列の中から1つずつ取り出して「goal」という名前をつける -->
      <!-- :key は「どれがどのデータか」Vueが迷わないためのマイナンバー（id）を渡す -->
      <li v-for="goal in goals" :key="goal.id">
        
        <!-- 目標のタイトルを RouterLink で囲んで、クリックできるようにする -->
        <!-- 「:to」を使って、JavaScriptの変数（goal.id）をURLに埋め込む -->
        <!-- <RouterLink :to="`/goals/${goal.id}/calendar`"> -->
        
         <!-- こっちの方が汎用性ある -->
        <RouterLink :to="{ name: 'calendar', params: { goalId: goal.id }}">
        {{ goal.title }}
        </RouterLink>
        
      </li>
    </ul>

    <!-- 【ここから追加】カレンダーのポップアップ（モーダル） -->
    <div v-if="isCalendarOpen" class="modal-overlay" @click.self="closeCalendar">
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
</style>
