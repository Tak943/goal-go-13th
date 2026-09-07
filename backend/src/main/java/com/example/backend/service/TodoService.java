package com.example.backend.service;

import java.time.LocalDate;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import com.example.backend.entity.Todo;
import com.example.backend.repository.TodoRepository;

import jakarta.transaction.Transactional;

@Service
public class TodoService {
    private static final Logger logger = LoggerFactory.getLogger(TodoService.class);

    private final TodoRepository todoRepository;

    public TodoService(TodoRepository todoRepository) {
        this.todoRepository = todoRepository;
    }

    public List<Todo> getNotDoneTodoByGoalId(Long goalId) {
        return this.todoRepository.findByGoalIdAndIsDone(goalId, false);
    }

    public List<Todo> getAllTodo() {
        return this.todoRepository.findAll();
    }

    @Transactional
    public Todo createNewTodo(Long goalId, Todo todo) {
        todo.setGoalId(goalId);
        try {
            Todo savedTodo = this.todoRepository.save(todo);
            logger.info("GoalID: {} へのTodo保存に成功しました。新規TodoID: {}", goalId, savedTodo.getId());
            return savedTodo;
        } catch (Exception e) {
            logger.error("Todoの保存中にエラーが発生しました。GoalID: {}, 原因: {}", goalId, e.getMessage(), e);
            throw e;
        }
    }

    @Transactional
    public ResponseEntity<String> deleteTodoById(Long id) {
        try{
            this.todoRepository.deleteById(id);
            logger.info("TodoID: {} のデータを削除しました", id);
            return ResponseEntity.ok("id=" + id + "のtodoアイテムを削除");
        } catch(Exception e){
            logger.error("Todoの削除中にエラーが発生しました。TodoID: {}, 原因: {}", id, e.getMessage(), e);
            throw e;
        }
    }

    @Transactional
    public Todo modifyTodoById(Long id, Todo requestTodo) {
        Todo dbTodo = this.todoRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("id=" + id + "のtodoアイテムはありません"));
        if(requestTodo.getTitle() != null){
            dbTodo.setTitle(requestTodo.getTitle());
        }
        if(requestTodo.getTime() != null){
            dbTodo.setTime(requestTodo.getTime());
        }

        try {
            Todo updatedTodo = this.todoRepository.save(dbTodo);
            logger.info("TodoID: {} の内容を更新しました。", id);
            return updatedTodo;
        } catch (Exception e) {
            logger.error("Todoの更新中にエラーが発生しました。TodoID: {}, 原因: {}", id, e.getMessage(), e);
            throw e;
        }
    }

    public boolean isTodoGoalIdSameRequestGoalId(Long todoId, Long goalId){
        Todo dbTodo = this.todoRepository.findById(todoId)
        .orElseThrow(() -> new RuntimeException("id=" + todoId + "のtodoアイテムはありません"));
        return dbTodo.getGoalId().equals(goalId);
    }

    @Transactional
    public Todo completeTodoById(Long id){
        Todo dbTodo = this.todoRepository.findById(id)
        .orElseThrow(() -> new RuntimeException("id=" + id + "のtodoアイテムはありません"));
        dbTodo.setIsDone(true);
        dbTodo.setDoneDate(LocalDate.now());

        try {
            Todo completedTodo = this.todoRepository.save(dbTodo);
            logger.info("TodoID: {} を「完了」状態に更新しました。", id);
            return completedTodo;
        } catch (Exception e) {
            logger.error("Todoの完了処理中にエラーが発生しました。TodoID: {}, 原因: {}", id, e.getMessage(), e);
            throw e;
        }
    }

    public List<Todo> getDoneTodo(Long goalId){
        return this.todoRepository.findByGoalIdAndIsDone(goalId, true);
    }
}
