package com.example.backend.controller;

import com.example.backend.service.UserService;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;

import com.example.backend.entity.Goal;
import com.example.backend.entity.User;
import com.example.backend.repository.GoalRepository;
import com.example.backend.repository.MarkRepository;
import com.example.backend.service.GoalService;
import com.example.backend.service.MarkService;

import java.util.List;
import java.util.Optional;

import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestParam;

@RestController
@RequestMapping("/api/goals")
@CrossOrigin
public class GoalController {

    private final UserService userService;
    private final GoalService goalService;
    private final MarkService markService;

    public GoalController(GoalService goalService, MarkService markService, UserService userService) {
        this.goalService = goalService;
        this.markService = markService;
        this.userService = userService;
    }

    @GetMapping
    public List<Goal> getGoals(
            @RequestParam(name = "userId", required = false) Long userId,
            @RequestHeader("Authorization") String token) {
        User requestUser = this.userService.getUserByToken(token)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "ユーザーが見つかりません"));
        if (userId != null) {
            if (requestUser.getId() == userId) {
                return this.goalService.getAllGoalsByUserId(userId);
            } else {
                throw new ResponseStatusException(
                        HttpStatus.FORBIDDEN, "アクセス拒否");
            }
        }
        // 全件取得はあとで削除
        else {
            return this.goalService.getAllGoals();
        }
    }

    // デバッグ用
    // @GetMapping("/{id}")
    // public Optional<Goal> getGoalById(@PathVariable("id") Long id) {
    // return goalService.getGoalById(id);
    // }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED) // HTTP 201 (正常Created) 今後ステータスコードはいったん無視。基幹機能に集中。
    public Goal createGoal(
            @RequestBody Goal goal,
            @RequestHeader("Authorization") String token) {
        return goalService.createGoal(goal, token);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<String> deleteGoalById(
            @PathVariable("id") Long id,
            @RequestHeader("Authorization") String token) {
        if (goalService.isAuthorizedUser(id, token)) {
            markService.deleteMarkById(id);
            return goalService.deleteGoalById(id);
        } else {
            throw new ResponseStatusException(
                    HttpStatus.FORBIDDEN, "アクセス拒否");
        }
    }

    @PutMapping("/{id}")
    public ResponseEntity<Goal> modifyGoalById(
            @PathVariable("id") Long id,
            @RequestBody Goal newGoalData,
            @RequestHeader("Authorization") String token) {
        if (goalService.isAuthorizedUser(id, token)) {
            Goal updatedGoal = goalService.modifyGoalById(id, newGoalData);
            return ResponseEntity.ok(updatedGoal);
        } else {
            throw new ResponseStatusException(
                    HttpStatus.FORBIDDEN, "アクセス拒否");
        }
    }

}
