package com.example.backend.service;

import java.util.List;
import java.util.Optional;

import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.server.ResponseStatusException;

import com.example.backend.controller.MarkController;
import com.example.backend.entity.Goal;
import com.example.backend.repository.GoalRepository;

import jakarta.transaction.Transactional;

@Service
public class GoalService {
    private final GoalRepository goalRepository;

    public GoalService(GoalRepository goalRepository) {
        this.goalRepository = goalRepository;
    }

    public List<Goal> getAllGoals() {
        return this.goalRepository.findAll();
    }

    public Optional<Goal> getGoalById(@PathVariable("id") Long id) {
        return this.goalRepository.findById(id);
    }

    // startDate <= targetDateではない。すなわち startDate > targetDate のときエラー
    public Goal createGoal(@RequestBody Goal goal) {
        if(goal.getStartDate().isAfter(goal.getTargetDate())){
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "開始日は目標日より後に設定できません");
        }
        return this.goalRepository.save(goal);
    }

    @Transactional
    public ResponseEntity<String> deleteGoalById(@PathVariable("id") Long id) {
        this.goalRepository.deleteById(id);
        return ResponseEntity.ok("Id=" + id + "の目標とマークを削除");
    }

    @Transactional
    public Goal modifyGoalById(Long id, Goal newGoalData){
        if(newGoalData.getStartDate().isAfter(newGoalData.getTargetDate())){
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "開始日は目標日より後に設定できません");
        }
        Goal currentGoalData = getGoalById(id)
        .orElseThrow(() -> new RuntimeException("id=" + id + "のデータはありません"));
        
        currentGoalData.setTitle(newGoalData.getTitle());
        currentGoalData.setStartDate(newGoalData.getStartDate());
        currentGoalData.setTargetDate(newGoalData.getTargetDate());

        return this.goalRepository.save(currentGoalData);
    }
}
