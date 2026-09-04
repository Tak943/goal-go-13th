package com.example.backend.controller;

import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;

import com.example.backend.entity.Goal;
import com.example.backend.repository.GoalRepository;

import java.util.List;
import java.util.Optional;

import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.http.HttpStatus;

@RestController
@RequestMapping("/api/goals")
@CrossOrigin
public class GoalController {

    private final GoalRepository goalRepository;

    public GoalController(GoalRepository goalRepository) {
        this.goalRepository = goalRepository;
    }

    @GetMapping
    public List<Goal> getAllGoals() {
        return this.goalRepository.findAll();
    }

    @GetMapping("/{id}")
    public Optional<Goal> getGoalById(@PathVariable("id") Long id){
        return this.goalRepository.findById(id);
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED) // HTTP 201 (正常Created) 今後ステータスコードはいったん無視。基幹機能に集中。
    public Goal createGoal(@RequestBody Goal goal) {

        return this.goalRepository.save(goal);
    }

}
