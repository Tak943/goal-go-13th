package com.example.backend.service;

import java.util.List;

import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;

import com.example.backend.entity.Mark;
import com.example.backend.repository.MarkRepository;

import jakarta.transaction.Transactional;

@Service
public class MarkService {
    private final MarkRepository markRepository;

    public MarkService(MarkRepository markRepository) {
        this.markRepository = markRepository;
    }

    public List<Mark> getAllMarkByGoalId(@PathVariable("goalId") Long goalId) {
        return this.markRepository.findByGoalId(goalId);
    }

    public Mark createNewMark(@PathVariable("goalId") Long goalId, @RequestBody Mark mark) {
        mark.setGoalId(goalId);

        return this.markRepository.save(mark);
    }

    @Transactional
    public ResponseEntity<String> deleteMarkById(Long goalId) {
        this.markRepository.deleteByGoalId(goalId);
        return ResponseEntity.ok("id=" + goalId + "のマークを削除");
    }
}