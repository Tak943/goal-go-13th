<script setup>
import { ref, computed, onMounted } from 'vue';
import axios from 'axios';

const props = defineProps({
  goalId: {
    type: [String, Number],
    required: true
  }
});

const todos = ref([]);
const completedTodos = ref([]);

const newTodoTitle = ref('');
const newTodoTime = ref('');

const editingTodoId = ref(null);
const editingTitle = ref('');
const editingTime = ref('');

const showCompleteModal = ref(false);
const showDeleteModal = ref(false);
const selectedTodo = ref(null);

const showEditModal = ref(false);

const toastMessage = ref('');
const showToast = ref(false);

const token = localStorage.getItem('app_token');


// =========================
// Todo取得
// =========================

const fetchTodos = async () => {
  try {
    const response = await axios.get(
      `/api/goals/${props.goalId}/todo`,
      {
        headers: {
          Authorization: token
        }
      }
    );

    todos.value = response.data;
  } catch (error) {
    console.error('Todo取得エラー:', error);
    showToastMessage('Todoの取得に失敗しました');
  }
};


const fetchCompletedTodos = async () => {
  try {
    const response = await axios.get(
      `/api/goals/${props.goalId}/todo/complete`,
      {
        headers: {
          Authorization: token
        }
      }
    );

    completedTodos.value = response.data;
  } catch (error) {
    console.error('達成Todo取得エラー:', error);
  }
};


// =========================
// 時間順に並べる
// =========================

const sortedTodos = computed(() => {
  return [...todos.value].sort((a, b) => {
    return a.time.localeCompare(b.time);
  });
});


// 達成済みは達成日が新しい順
const sortedCompletedTodos = computed(() => {
  return [...completedTodos.value].sort((a, b) => {
    if (a.doneDate === b.doneDate) {
      return a.time.localeCompare(b.time);
    }

    return b.doneDate.localeCompare(a.doneDate);
  });
});


// =========================
// Todo追加
// =========================

const addTodo = async () => {
  if (!newTodoTitle.value.trim() || !newTodoTime.value) {
    showToastMessage('時間とTodoを入力してください');
    return;
  }

  try {
    await axios.post(
      `/api/goals/${props.goalId}/todo`,
      {
        title: newTodoTitle.value.trim(),
        time: newTodoTime.value
      },
      {
        headers: {
          Authorization: token
        }
      }
    );

    newTodoTitle.value = '';
    newTodoTime.value = '';

    await fetchTodos();

    showToastMessage('Todoを追加しました');
  } catch (error) {
    console.error('Todo追加エラー:', error);
    showToastMessage('Todoの追加に失敗しました');
  }
};


// =========================
// 編集
// =========================

const openEditModal = (todo) => {
  selectedTodo.value = todo;
  editingTodoId.value = todo.id;
  editingTitle.value = todo.title;
  editingTime.value = todo.time.substring(0, 5);

  showEditModal.value = true;
};


const closeEditModal = () => {
  showEditModal.value = false;
  selectedTodo.value = null;
};


const saveEdit = async () => {
  if (!editingTitle.value.trim() || !editingTime.value) {
    showToastMessage('時間とTodoを入力してください');
    return;
  }

  try {
    await axios.put(
      `/api/goals/${props.goalId}/todo/${editingTodoId.value}`,
      {
        title: editingTitle.value.trim(),
        time: editingTime.value
      },
      {
        headers: {
          Authorization: token
        }
      }
    );

    closeEditModal();
    await fetchTodos();

    showToastMessage('Todoを変更しました');
  } catch (error) {
    console.error('Todo変更エラー:', error);
    showToastMessage('Todoの変更に失敗しました');
  }
};


// =========================
// 完了
// =========================

const openCompleteModal = (todo) => {
  selectedTodo.value = todo;
  showCompleteModal.value = true;
};


const closeCompleteModal = () => {
  showCompleteModal.value = false;
  selectedTodo.value = null;
};


const completeTodo = async () => {
  if (!selectedTodo.value) {
    return;
  }

  try {
    await axios.put(
      `/api/goals/${props.goalId}/todo/${selectedTodo.value.id}/complete`,
      {},
      {
        headers: {
          Authorization: token
        }
      }
    );

    closeCompleteModal();

    await fetchTodos();
    await fetchCompletedTodos();

    showToastMessage('よくやった！');
  } catch (error) {
    console.error('Todo完了エラー:', error);
    showToastMessage('Todoの完了に失敗しました');
  }
};


// =========================
// 削除
// =========================

const openDeleteModal = (todo) => {
  selectedTodo.value = todo;
  showDeleteModal.value = true;
};


const closeDeleteModal = () => {
  showDeleteModal.value = false;
  selectedTodo.value = null;
};


const deleteTodo = async () => {
  if (!selectedTodo.value) {
    return;
  }

  try {
    await axios.delete(
      `/api/goals/${props.goalId}/todo/${selectedTodo.value.id}`,
      {
        headers: {
          Authorization: token
        }
      }
    );

    closeDeleteModal();

    await fetchTodos();

    showToastMessage('Todoを削除しました');
  } catch (error) {
    console.error('Todo削除エラー:', error);
    showToastMessage('Todoの削除に失敗しました');
  }
};


// =========================
// Toast
// =========================

const showToastMessage = (message) => {
  toastMessage.value = message;
  showToast.value = true;

  setTimeout(() => {
    showToast.value = false;
  }, 2500);
};


// =========================
// 時刻表示
// =========================

const formatTime = (time) => {
  if (!time) {
    return '';
  }

  return time.substring(0, 5);
};


onMounted(async () => {
  await fetchTodos();
  await fetchCompletedTodos();
});
</script>


<template>
  <section class="todo-section">

    <!-- ========================= -->
    <!-- Todoヘッダー -->
    <!-- ========================= -->

    <div class="todo-header">
      <div>
        <h2>今日のスケジュール</h2>
        <p>やることを時間順に管理しよう</p>
      </div>
    </div>


    <!-- ========================= -->
    <!-- Todo追加 -->
    <!-- ========================= -->

    <div class="todo-add-card">

      <div class="todo-input-row">

        <input
          v-model="newTodoTime"
          type="time"
          class="time-input"
        />

        <input
          v-model="newTodoTitle"
          type="text"
          class="title-input"
          placeholder="やることを入力"
          @keyup.enter="addTodo"
        />

        <button
          class="add-button"
          @click="addTodo"
        >
          ＋ 追加
        </button>

      </div>

    </div>


    <!-- ========================= -->
    <!-- 現在のTodo -->
    <!-- ========================= -->

    <div class="schedule-card">

      <div
        v-if="sortedTodos.length === 0"
        class="empty-todo"
      >
        <div class="empty-icon">✓</div>
        <p>今日のTodoはありません</p>
        <span>やることを追加してみよう！</span>
      </div>


      <div
        v-for="todo in sortedTodos"
        :key="todo.id"
        class="schedule-item"
      >

        <!-- 時間 -->
        <div class="schedule-time">
          {{ formatTime(todo.time) }}
        </div>


        <!-- タイムライン -->
        <div class="schedule-line">

          <div class="schedule-dot"></div>

        </div>


        <!-- Todo -->
        <div class="schedule-content">

          <div class="todo-card">

            <div class="todo-main">

              <input
                type="checkbox"
                class="todo-checkbox"
                @change="openCompleteModal(todo)"
              />

              <span class="todo-title">
                {{ todo.title }}
              </span>

            </div>


            <div class="todo-actions">

              <button
                class="edit-button"
                @click="openEditModal(todo)"
              >
                編集
              </button>

              <button
                class="delete-button"
                @click="openDeleteModal(todo)"
              >
                削除
              </button>

            </div>

          </div>

        </div>

      </div>

    </div>


    <!-- ========================= -->
    <!-- 達成リスト -->
    <!-- ========================= -->

    <div class="completed-section">

      <h2>達成リスト</h2>

      <div
        v-if="sortedCompletedTodos.length === 0"
        class="empty-completed"
      >
        まだ達成したTodoはありません
      </div>


      <div
        v-for="todo in sortedCompletedTodos"
        :key="todo.id"
        class="completed-item"
      >

        <div class="completed-date">
          {{ todo.doneDate }}
        </div>

        <div class="completed-time">
          {{ formatTime(todo.time) }}
        </div>

        <div class="completed-title">
          {{ todo.title }}
        </div>

        <div class="completed-check">
          ✓
        </div>

      </div>

    </div>


    <!-- ========================= -->
    <!-- 完了確認モーダル -->
    <!-- ========================= -->

    <div
      v-if="showCompleteModal"
      class="todo-modal-overlay"
      @click.self="closeCompleteModal"
    >

      <div class="todo-modal">

        <div class="modal-icon">🎉</div>

        <h3>よくやった！</h3>

        <p>
          「{{ selectedTodo?.title }}」を<br>
          完了にしますか？
        </p>

        <div class="modal-buttons">

          <button
            class="modal-cancel"
            @click="closeCompleteModal"
          >
            キャンセル
          </button>

          <button
            class="modal-primary"
            @click="completeTodo"
          >
            やったぜ！
          </button>

        </div>

      </div>

    </div>


    <!-- ========================= -->
    <!-- 編集モーダル -->
    <!-- ========================= -->

    <div
      v-if="showEditModal"
      class="todo-modal-overlay"
      @click.self="closeEditModal"
    >

      <div class="todo-modal">

        <h3>Todoを編集</h3>

        <div class="edit-form">

          <label>
            時間
          </label>

          <input
            v-model="editingTime"
            type="time"
          />

          <label>
            Todo
          </label>

          <input
            v-model="editingTitle"
            type="text"
          />

        </div>


        <div class="modal-buttons">

          <button
            class="modal-cancel"
            @click="closeEditModal"
          >
            キャンセル
          </button>

          <button
            class="modal-primary"
            @click="saveEdit"
          >
            保存
          </button>

        </div>

      </div>

    </div>


    <!-- ========================= -->
    <!-- 削除確認モーダル -->
    <!-- ========================= -->

    <div
      v-if="showDeleteModal"
      class="todo-modal-overlay"
      @click.self="closeDeleteModal"
    >

      <div class="todo-modal">

        <div class="modal-icon">🗑️</div>

        <h3>Todoを削除</h3>

        <p>
          「{{ selectedTodo?.title }}」を<br>
          削除しますか？
        </p>

        <div class="modal-buttons">

          <button
            class="modal-cancel"
            @click="closeDeleteModal"
          >
            キャンセル
          </button>

          <button
            class="modal-delete"
            @click="deleteTodo"
          >
            削除する
          </button>

        </div>

      </div>

    </div>


    <!-- ========================= -->
    <!-- Toast -->
    <!-- ========================= -->

    <div
      v-if="showToast"
      class="todo-toast"
    >
      {{ toastMessage }}
    </div>

  </section>
</template>


<style scoped>

.todo-section {
  width: min(1100px, calc(100% - 32px));
  margin: 32px auto 60px;
}

.todo-header {
  margin-bottom: 20px;
}

.todo-header h2,
.completed-section h2 {
  margin: 0;
  font-size: 24px;
  font-weight: 700;
}

.todo-header p {
  margin: 6px 0 0;
  color: #777;
  font-size: 14px;
}


/* =========================
   Todo追加
========================= */

.todo-add-card {
  background: white;
  border-radius: 16px;
  padding: 18px;
  margin-bottom: 20px;
  box-shadow: 0 4px 16px rgba(0, 0, 0, 0.06);
}

.todo-input-row {
  display: flex;
  gap: 10px;
}

.time-input,
.title-input {
  border: 1px solid #ddd;
  border-radius: 10px;
  padding: 12px;
  font-size: 15px;
  outline: none;
}

.time-input {
  width: 130px;
}

.title-input {
  flex: 1;
  min-width: 0;
}

.time-input:focus,
.title-input:focus {
  border-color: #777;
}

.add-button {
  border: none;
  border-radius: 10px;
  padding: 0 20px;
  background: #222;
  color: white;
  font-weight: 600;
  cursor: pointer;
}


/* =========================
   スケジュール
========================= */

.schedule-card {
  background: white;
  border-radius: 18px;
  padding: 24px 20px;
  box-shadow: 0 4px 16px rgba(0, 0, 0, 0.06);
}

.schedule-item {
  display: grid;
  grid-template-columns: 70px 24px 1fr;
  min-height: 76px;
}

.schedule-time {
  padding-top: 14px;
  font-size: 14px;
  font-weight: 700;
  color: #555;
  text-align: right;
  padding-right: 12px;
}

.schedule-line {
  position: relative;
  display: flex;
  justify-content: center;
}

.schedule-line::after {
  content: "";
  position: absolute;
  top: 0;
  bottom: 0;
  width: 2px;
  background: #e5e5e5;
}

.schedule-item:last-child .schedule-line::after {
  bottom: 50%;
}

.schedule-dot {
  position: relative;
  z-index: 2;
  width: 10px;
  height: 10px;
  margin-top: 17px;
  border-radius: 50%;
  background: #222;
}

.schedule-content {
  padding: 4px 0 16px 14px;
}

.todo-card {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 12px;
  padding: 14px 16px;
  border: 1px solid #eee;
  border-radius: 12px;
  background: #fafafa;
}

.todo-main {
  display: flex;
  align-items: center;
  gap: 12px;
  min-width: 0;
}

.todo-checkbox {
  width: 20px;
  height: 20px;
  cursor: pointer;
  flex-shrink: 0;
}

.todo-title {
  font-size: 15px;
  word-break: break-word;
}

.todo-actions {
  display: flex;
  gap: 6px;
  flex-shrink: 0;
}

.edit-button,
.delete-button {
  border: none;
  background: transparent;
  cursor: pointer;
  padding: 7px 8px;
  border-radius: 7px;
}

.edit-button {
  color: #555;
}

.delete-button {
  color: #b33;
}

.edit-button:hover,
.delete-button:hover {
  background: #eee;
}


/* =========================
   空状態
========================= */

.empty-todo {
  text-align: center;
  padding: 50px 20px;
  color: #777;
}

.empty-icon {
  font-size: 32px;
  margin-bottom: 10px;
}

.empty-todo p {
  margin: 0 0 5px;
  font-weight: 600;
}

.empty-todo span {
  font-size: 13px;
}


/* =========================
   達成リスト
========================= */

.completed-section {
  margin-top: 36px;
}

.empty-completed {
  margin-top: 14px;
  padding: 25px;
  background: white;
  border-radius: 14px;
  color: #888;
  text-align: center;
}

.completed-item {
  display: grid;
  grid-template-columns: 100px 70px 1fr 30px;
  align-items: center;
  gap: 12px;

  margin-top: 10px;
  padding: 14px 16px;

  background: white;
  border-radius: 12px;
  box-shadow: 0 3px 12px rgba(0, 0, 0, 0.04);
}

.completed-date,
.completed-time {
  color: #777;
  font-size: 13px;
}

.completed-title {
  text-decoration: line-through;
  color: #777;
}

.completed-check {
  font-size: 18px;
  text-align: center;
}


/* =========================
   モーダル
========================= */

.todo-modal-overlay {
  position: fixed;
  inset: 0;
  z-index: 2500;

  display: flex;
  align-items: center;
  justify-content: center;

  padding: 20px;

  background: rgba(0, 0, 0, 0.45);
}

.todo-modal {
  width: min(420px, 100%);
  padding: 28px;
  border-radius: 18px;
  background: white;
  box-shadow: 0 10px 40px rgba(0, 0, 0, 0.2);
  text-align: center;
}

.todo-modal h3 {
  margin: 8px 0 12px;
  font-size: 22px;
}

.todo-modal p {
  margin: 0;
  line-height: 1.7;
  color: #666;
}

.modal-icon {
  font-size: 38px;
}

.modal-buttons {
  display: flex;
  gap: 10px;
  margin-top: 24px;
}

.modal-buttons button {
  flex: 1;
  padding: 12px;
  border: none;
  border-radius: 10px;
  font-weight: 600;
  cursor: pointer;
}

.modal-cancel {
  background: #eee;
  color: #444;
}

.modal-primary {
  background: #222;
  color: white;
}

.modal-delete {
  background: #b33;
  color: white;
}


/* =========================
   編集フォーム
========================= */

.edit-form {
  display: flex;
  flex-direction: column;
  gap: 8px;
  text-align: left;
  margin-top: 20px;
}

.edit-form label {
  font-size: 13px;
  color: #666;
}

.edit-form input {
  padding: 11px;
  border: 1px solid #ddd;
  border-radius: 9px;
  font-size: 15px;
  margin-bottom: 8px;
}


/* =========================
   Toast
========================= */

.todo-toast {
  position: fixed;
  left: 50%;
  bottom: 30px;
  transform: translateX(-50%);

  z-index: 4000;

  padding: 12px 20px;
  border-radius: 10px;

  background: #222;
  color: white;

  font-size: 14px;
}


/* =========================
   スマホ
========================= */

@media (max-width: 600px) {

  .todo-section {
    width: calc(100% - 20px);
  }

  .todo-input-row {
    display: grid;
    grid-template-columns: 110px 1fr;
  }

  .time-input {
    width: auto;
  }

  .add-button {
    grid-column: 1 / -1;
    height: 44px;
  }

  .schedule-card {
    padding: 18px 10px;
  }

  .schedule-item {
    grid-template-columns: 55px 20px 1fr;
  }

  .schedule-time {
    font-size: 12px;
    padding-right: 8px;
  }

  .schedule-content {
    padding-left: 10px;
  }

  .todo-card {
    display: block;
  }

  .todo-actions {
    margin-top: 10px;
    justify-content: flex-end;
  }

  .completed-item {
    grid-template-columns: 1fr auto;
  }

  .completed-date {
    grid-column: 1;
  }

  .completed-time {
    grid-column: 2;
  }

  .completed-title {
    grid-column: 1;
  }

  .completed-check {
    grid-column: 2;
    grid-row: 2;
  }
}

</style>