package com.example.backend.controller;

import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;

import com.example.backend.entity.Goal;
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
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PutMapping;

@RestController
@RequestMapping("/api/goals")
@CrossOrigin
public class GoalController {

    private final GoalService goalService;
    private final MarkService markService;

    public GoalController(GoalService goalService, MarkService markService) {
        this.goalService = goalService;
        this.markService = markService;
    }

    @GetMapping
    public List<Goal> getAllGoals() {
        return goalService.getAllGoals();
    }

    @GetMapping("/{id}")
    public Optional<Goal> getGoalById(@PathVariable("id") Long id) {
        return goalService.getGoalById(id);
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED) // HTTP 201 (正常Created) 今後ステータスコードはいったん無視。基幹機能に集中。
    public Goal createGoal(@RequestBody Goal goal) {

        return goalService.createGoal(goal);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<String> deleteGoalById(@PathVariable("id") Long id) {
        markService.deleteMarkById(id);
        return goalService.deleteGoalById(id);
    }

}
