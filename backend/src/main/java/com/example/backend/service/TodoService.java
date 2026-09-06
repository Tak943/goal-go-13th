package com.example.backend.service;

import java.time.LocalDate;
import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import com.example.backend.entity.Todo;
import com.example.backend.repository.TodoRepository;

import jakarta.transaction.Transactional;

@Service
public class TodoService {
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
        return this.todoRepository.save(todo);
    }

    @Transactional
    public ResponseEntity<String> deleteTodoById(Long id) {
        this.todoRepository.deleteById(id);
        return ResponseEntity.ok("id=" + id + "のtodoアイテムを削除");
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

        return this.todoRepository.save(dbTodo);
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

        return this.todoRepository.save(dbTodo);
    }

    public List<Todo> getDoneTodo(Long goalId){
        return this.todoRepository.findByGoalIdAndIsDone(goalId, true);
    }
}
