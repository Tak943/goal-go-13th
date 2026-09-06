package com.example.backend.controller;

import com.example.backend.service.GoalService;
import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

import com.example.backend.entity.Todo;
import com.example.backend.service.TodoService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.PutMapping;

@RestController
@RequestMapping("/api/goals/{goalId}/todo")
@CrossOrigin
public class TodoController {
    private final GoalService goalService;
    private final TodoService todoService;

    public TodoController(TodoService todoService, GoalService goalService) {
        this.todoService = todoService;
        this.goalService = goalService;
    }

    @GetMapping
    public List<Todo> getNotDoneTodoByGoalId(
            @PathVariable("goalId") Long goalId,
            @RequestHeader("Authorization") String token,
            @RequestParam(name = "all", required = false) boolean all) {
        if (this.goalService.isAuthorizedUser(goalId, token)) {
            if (!all) {
                // goalIdごとのリスト一覧取得
                return this.todoService.getNotDoneTodoByGoalId(goalId);
            } else {
                // 全リスト取得。デバッグ用
                return this.todoService.getAllTodo();
            }
        } else {
            throw new ResponseStatusException(
                    HttpStatus.FORBIDDEN, "アクセス拒否");
        }
    }

    @PostMapping
    public Todo createNewTodo(
            @PathVariable("goalId") Long goalId,
            @RequestBody Todo todo,
            @RequestHeader("Authorization") String token) {
        if (this.goalService.isAuthorizedUser(goalId, token)) {
            return this.todoService.createNewTodo(goalId, todo);
        } else {
            throw new ResponseStatusException(
                    HttpStatus.FORBIDDEN, "アクセス拒否");
        }
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<String> deleteTodoById(
            @PathVariable("goalId") Long goalId,
            @PathVariable("id") Long id,
            @RequestHeader("Authorization") String token) {
        if (this.goalService.isAuthorizedUser(goalId, token)
                && this.todoService.isTodoGoalIdSameRequestGoalId(id, goalId)) {
            return this.todoService.deleteTodoById(id);
        } else {
            throw new ResponseStatusException(
                    HttpStatus.FORBIDDEN, "アクセス拒否");
        }
    }

    @PutMapping("/{id}")
    public Todo modifyTodoById(
            @PathVariable("goalId") Long goalId,
            @PathVariable("id") Long id,
            @RequestBody Todo todo,
            @RequestHeader("Authorization") String token) {
        if (this.goalService.isAuthorizedUser(goalId, token)
                && this.todoService.isTodoGoalIdSameRequestGoalId(id, goalId)) {
            return this.todoService.modifyTodoById(id, todo);
        } else {
            throw new ResponseStatusException(
                    HttpStatus.FORBIDDEN, "アクセス拒否");
        }
    }

    @PutMapping("/{id}/complete")
    public Todo completeTodoById(
        @PathVariable("goalId") Long goalId,
        @PathVariable("id") Long id,
        @RequestHeader("Authorization") String token
    ) {
        if (this.goalService.isAuthorizedUser(goalId, token)
                && this.todoService.isTodoGoalIdSameRequestGoalId(id, goalId)) {
            return this.todoService.completeTodoById(id);
        } else {
            throw new ResponseStatusException(
                    HttpStatus.FORBIDDEN, "アクセス拒否");
        }
    }

    @GetMapping("/complete")
    public List<Todo> getDoneTodo(
        @PathVariable("goalId") Long goalId,
        @RequestHeader("Authorization") String token
    ) {
        if (this.goalService.isAuthorizedUser(goalId, token)) {
            return this.todoService.getDoneTodo(goalId);
        } else {
            throw new ResponseStatusException(
                    HttpStatus.FORBIDDEN, "アクセス拒否");
        }
    }
    
    

}
