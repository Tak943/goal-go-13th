package com.example.backend.controller;

import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.example.backend.repository.MarkRepository;
import com.example.backend.service.MarkService;
import com.example.backend.entity.Mark;

import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

import java.util.List;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;



@RestController
@RequestMapping("/api/goals/{goalId}/marks")
@CrossOrigin
public class MarkController {
    private final MarkService markService;

    public MarkController(MarkService markService){
        this.markService = markService;
    }

    @GetMapping
    public List<Mark> getAllMarkByGoalId(@PathVariable("goalId") Long goalId) {
        return markService.getAllMarkByGoalId(goalId);
    }
    
    @PostMapping
    public Mark createNewMark(@PathVariable("goalId") Long goalId, @RequestBody Mark mark) {
        return markService.createNewMark(goalId, mark);
    }
}
