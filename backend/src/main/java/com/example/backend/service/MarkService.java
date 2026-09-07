package com.example.backend.service;

import java.util.List;

import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;

import com.example.backend.entity.Mark;
import com.example.backend.repository.MarkRepository;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import jakarta.transaction.Transactional;

@Service
public class MarkService {
    private static final Logger logger = LoggerFactory.getLogger(MarkService.class);

    private final MarkRepository markRepository;

    public MarkService(MarkRepository markRepository) {
        this.markRepository = markRepository;
    }

    public List<Mark> getAllMarkByGoalId(@PathVariable("goalId") Long goalId) {
        return this.markRepository.findByGoalId(goalId);
    }

    @Transactional
    public Mark createNewMark(@PathVariable("goalId") Long goalId, @RequestBody Mark mark) {
        mark.setGoalId(goalId);

        try {
            Mark savedMark = this.markRepository.save(mark);
            logger.info("GoalID: {} へのマーク保存に成功しました。新規MarkID: {}", goalId, savedMark.getId());
            return savedMark;
        } catch (Exception e) {
            //親（Goal）が存在しないのに子（Mark）を保存しようとした時のエラーなどを検知
            logger.error("マークの保存中にエラーが発生しました。GoalID: {}, 原因: {}", goalId, e.getMessage(), e);
            throw e;
        }
    }

    @Transactional
    public ResponseEntity<String> deleteMarkById(Long goalId) {
        try {
            this.markRepository.deleteByGoalId(goalId);
            logger.info("GoalID: {} に紐づくすべてのマークデータを削除しました。", goalId);
            return ResponseEntity.ok("id=" + goalId + "のマークを削除");
        } catch (Exception e) {
            logger.error("マークの削除中にエラーが発生しました。GoalID: {}, 原因: {}", goalId, e.getMessage(), e);
            throw e;
        }
    }
}