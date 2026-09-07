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

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

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

    private static final Logger logger = LoggerFactory.getLogger(GoalController.class);

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
                //ハックしようとしたのは重大だが、システム自体は落ちない＝バグではない、からerrorではなくwarnにしておく。夜中でも担当エンジニアをたたき起こさないレベル
                logger.warn("権限エラー：他人の目標一覧へのアクセス試行を検知しました。userId: {}", userId);
                throw new ResponseStatusException(
                        HttpStatus.FORBIDDEN, "アクセス拒否");
            }
        }
        // 全件取得はあとで削除。デバッグ用
        else {
            return this.goalService.getAllGoals();
        }
    }

    @GetMapping("/{id}")
    public Optional<Goal> getGoalById(
            @PathVariable("id") Long id,
            @RequestHeader("Authorization") String token) {
        if (goalService.isAuthorizedUser(id, token)) {
            return goalService.getGoalById(id);
        } else {
            logger.warn("権限エラー：他人の目標詳細へのアクセス試行。GoalID: {}", id);
            throw new ResponseStatusException(
                    HttpStatus.FORBIDDEN, "アクセス拒否");
        }
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED) // HTTP 201 (正常Created) 今後ステータスコードはいったん無視。基幹機能に集中。
    public Goal createGoal(
            @RequestBody Goal goal,
            @RequestHeader("Authorization") String token) {
        logger.info("目標作成リクエストを受信。タイトル: {}", goal.getTitle());
        return goalService.createGoal(goal, token);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<String> deleteGoalById(
            @PathVariable("id") Long id,
            @RequestHeader("Authorization") String token) {
        logger.info("目標削除リクエストを受信。GoalID: {}", id);

        if (goalService.isAuthorizedUser(id, token)) {
            logger.info("権限チェックOK。GoalID: {} の削除処理を開始します。", id);
            markService.deleteMarkById(id);
            return goalService.deleteGoalById(id);
        } else {
            logger.warn("権限エラー：不正なトークンでの目標削除試行。GoalID: {}", id);
            throw new ResponseStatusException(
                    HttpStatus.FORBIDDEN, "アクセス拒否");
        }
    }

    @PutMapping("/{id}")
    public ResponseEntity<Goal> modifyGoalById(
            @PathVariable("id") Long id,
            @RequestBody Goal newGoalData,
            @RequestHeader("Authorization") String token) {
        logger.info("目標更新リクエストを受信。GoalID: {}", id);

        if (goalService.isAuthorizedUser(id, token)) {
            logger.info("権限チェックOK。GoalID: {} の更新処理を開始します。", id);
            Goal updatedGoal = goalService.modifyGoalById(id, newGoalData);
            return ResponseEntity.ok(updatedGoal);
        } else {
            logger.warn("権限エラー：不正なトークンでの目標更新試行。GoalID: {}", id);
            throw new ResponseStatusException(
                    HttpStatus.FORBIDDEN, "アクセス拒否");
        }
    }

}
