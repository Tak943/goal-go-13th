package com.example.backend.controller;

import com.example.backend.service.GoalService;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

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
    private static final Logger logger = LoggerFactory.getLogger(TodoController.class);

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
        logger.info("GoalID: {} の未完了Todo一覧取得リクエストを受信 (all={})", goalId, all);
        
        if (this.goalService.isAuthorizedUser(goalId, token)) {
            if (!all) {
                // goalIdごとのリスト一覧取得
                return this.todoService.getNotDoneTodoByGoalId(goalId);
            } else {
                // 全リスト取得。デバッグ用
                return this.todoService.getAllTodo();
            }
        } else {
            logger.warn("権限エラー：他人の未完了Todo一覧へのアクセス試行。GoalID: {}", goalId);
            throw new ResponseStatusException(
                    HttpStatus.FORBIDDEN, "アクセス拒否");
        }
    }

    @PostMapping
    public Todo createNewTodo(
            @PathVariable("goalId") Long goalId,
            @RequestBody Todo todo,
            @RequestHeader("Authorization") String token) {
        logger.info("GoalID: {} への新規Todo追加リクエストを受信。タイトル: {}", goalId, todo.getTitle());

        if (this.goalService.isAuthorizedUser(goalId, token)) {
            return this.todoService.createNewTodo(goalId, todo);
        } else {
            logger.warn("権限エラー：不正なトークンでのTodo追加試行。GoalID: {}", goalId);
            throw new ResponseStatusException(
                    HttpStatus.FORBIDDEN, "アクセス拒否");
        }
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<String> deleteTodoById(
            @PathVariable("goalId") Long goalId,
            @PathVariable("id") Long id,
            @RequestHeader("Authorization") String token) {
        logger.info("GoalID: {} のTodo(ID: {})削除リクエストを受信", goalId, id);

        if (this.goalService.isAuthorizedUser(goalId, token)
                && this.todoService.isTodoGoalIdSameRequestGoalId(id, goalId)) {
            return this.todoService.deleteTodoById(id);
        } else {
            logger.warn("権限エラー：不正アクセス、または他GoalのTodo削除試行。GoalID: {}, TodoID: {}", goalId, id);
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
        logger.info("GoalID: {} のTodo(ID: {})更新リクエストを受信", goalId, id);

        if (this.goalService.isAuthorizedUser(goalId, token)
                && this.todoService.isTodoGoalIdSameRequestGoalId(id, goalId)) {
            return this.todoService.modifyTodoById(id, todo);
        } else {
            logger.warn("権限エラー：不正アクセス、または他GoalのTodo更新試行。GoalID: {}, TodoID: {}", goalId, id);
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
        logger.info("GoalID: {} のTodo(ID: {})完了処理リクエストを受信", goalId, id);

        if (this.goalService.isAuthorizedUser(goalId, token)
                && this.todoService.isTodoGoalIdSameRequestGoalId(id, goalId)) {
            return this.todoService.completeTodoById(id);
        } else {
            logger.warn("権限エラー：不正アクセス、または他GoalのTodo完了試行。GoalID: {}, TodoID: {}", goalId, id);
            throw new ResponseStatusException(
                    HttpStatus.FORBIDDEN, "アクセス拒否");
        }
    }

    @GetMapping("/complete")
    public List<Todo> getDoneTodo(
        @PathVariable("goalId") Long goalId,
        @RequestHeader("Authorization") String token
    ) {
        logger.info("GoalID: {} の完了済みTodo一覧取得リクエストを受信", goalId);

        if (this.goalService.isAuthorizedUser(goalId, token)) {
            return this.todoService.getDoneTodo(goalId);
        } else {
            logger.warn("権限エラー：他人の完了済みTodo一覧へのアクセス試行。GoalID: {}", goalId);
            throw new ResponseStatusException(
                    HttpStatus.FORBIDDEN, "アクセス拒否");
        }
    }
    
    

}
