package com.example.backend.service;

import java.util.List;
import java.util.Optional;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.server.ResponseStatusException;

import com.example.backend.controller.MarkController;
import com.example.backend.entity.Goal;
import com.example.backend.entity.User;
import com.example.backend.repository.GoalRepository;

import jakarta.transaction.Transactional;

@Service
public class GoalService {
    private static final Logger logger = LoggerFactory.getLogger(GoalService.class);

    private final UserService userService;
    private final GoalRepository goalRepository;

    public GoalService(GoalRepository goalRepository, UserService userService) {
        this.goalRepository = goalRepository;
        this.userService = userService;
    }

    public List<Goal> getAllGoals() {
        return this.goalRepository.findAll();
    }

    public Optional<Goal> getGoalById(@PathVariable("id") Long id) {
        return this.goalRepository.findById(id);
    }

    public List<Goal> getAllGoalsByUserId(Long userId) {
        return this.goalRepository.findByUserId(userId);
    }

    @Transactional
    public Goal createGoal(Goal goal, String token) {
        User requsetUser = this.userService.getUserByToken(token)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "ユーザーが見つかりません"));

        // startDate <= targetDateではない。すなわち startDate > targetDate のときエラー
        if (goal.getStartDate().isAfter(goal.getTargetDate())) {
            logger.warn("バリデーションエラー: 開始日が目標日より後に設定されています");
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "開始日は目標日より後に設定できません");
        }

        goal.setUserId(requsetUser.getId());
        try {
            Goal savedGoal = this.goalRepository.save(goal);
            logger.info("目標の保存に成功しました。新規GoalID: {}", savedGoal.getId());
            return savedGoal;
        } catch (Exception e) {
            logger.error("目標の保存中にデータベースエラーが発生しました: {}", e.getMessage(), e);
            throw e;
        }
    }

    @Transactional
    public ResponseEntity<String> deleteGoalById(@PathVariable("id") Long id) {
        try {
            this.goalRepository.deleteById(id);
            logger.info("GoalID: {} の目標データを削除しました。", id);
            return ResponseEntity.ok("Id=" + id + "の目標とマークを削除");
        } catch (Exception e) {
            logger.error("目標の削除中にエラーが発生しました。GoalID: {}, 原因: {}", id, e.getMessage(), e);
            throw e;
        }
    }

    @Transactional
    public Goal modifyGoalById(Long id, Goal newGoalData) {
        if (newGoalData.getStartDate().isAfter(newGoalData.getTargetDate())) {
            logger.warn("バリデーションエラー: 更新時の開始日が目標日より後です。GoalID: {}", id);
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "開始日は目標日より後に設定できません");
        }
        Goal currentGoalData = getGoalById(id)
                .orElseThrow(() -> new RuntimeException("id=" + id + "のデータはありません"));

        currentGoalData.setTitle(newGoalData.getTitle());
        currentGoalData.setStartDate(newGoalData.getStartDate());
        currentGoalData.setTargetDate(newGoalData.getTargetDate());

        try {
            Goal updatedGoal = this.goalRepository.save(currentGoalData);
            logger.info("GoalID: {} の更新に成功しました。", id);
            return updatedGoal;
        } catch (Exception e) {
            logger.error("目標の更新中にエラーが発生しました。GoalID: {}, 原因: {}", id, e.getMessage(), e);
            throw e;
        }
    }

    public Boolean isAuthorizedUser(Long id, String token) {
        Goal goal = getGoalById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "目標が見つかりません"));
        Long requestUserId = goal.getUserId();

        User dbUser = userService.getUserById(requestUserId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "ユーザーが見つかりません"));
        return dbUser.getToken().equals(token);
    }
}
