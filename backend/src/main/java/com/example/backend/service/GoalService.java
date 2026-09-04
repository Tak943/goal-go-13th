package com.example.backend.service;

import java.util.List;
import java.util.Optional;

import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;

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

    public Goal createGoal(@RequestBody Goal goal) {

        return this.goalRepository.save(goal);
    }

    @Transactional
    public ResponseEntity<String> deleteGoalById(@PathVariable("id") Long id) {
        this.goalRepository.deleteById(id);
        return ResponseEntity.ok("Id=" + id + "の目標とマークを削除");
    }
}
