package com.example.backend.controller;

import com.example.backend.service.GoalService;
import com.example.backend.service.UserService;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

import com.example.backend.repository.MarkRepository;
import com.example.backend.service.MarkService;
import com.example.backend.entity.Mark;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

import java.util.List;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;

@RestController
@RequestMapping("/api/goals/{goalId}/marks")
@CrossOrigin
public class MarkController {
    private final GoalService goalService;
    private final MarkService markService;

    public MarkController(MarkService markService, GoalService goalService) {
        this.markService = markService;
        this.goalService = goalService;
    }

    @GetMapping
    public List<Mark> getAllMarkByGoalId(
            @PathVariable("goalId") Long goalId,
            @RequestHeader("Authorization") String token) {
        if (this.goalService.isAuthorizedUser(goalId, token)) {
            return markService.getAllMarkByGoalId(goalId);
        } else {
            throw new ResponseStatusException(
                    HttpStatus.FORBIDDEN, "アクセス拒否");
        }
    }

    @PostMapping
    public Mark createNewMark(
            @PathVariable("goalId") Long goalId,
            @RequestBody Mark mark,
            @RequestHeader("Authorization") String token) {
        if (this.goalService.isAuthorizedUser(goalId, token)) {
            return markService.createNewMark(goalId, mark);
        } else {
            throw new ResponseStatusException(
                    HttpStatus.FORBIDDEN, "アクセス拒否");
        }
    }
}
