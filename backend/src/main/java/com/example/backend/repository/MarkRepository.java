package com.example.backend.repository;

import org.springframework.data.jpa.repository.JpaRepository;

import com.example.backend.entity.Mark;

import java.util.List;

public interface MarkRepository extends JpaRepository<Mark, Long>{
    List<Mark> findByGoalId(Long goalId);
    void deleteByGoalId(Long goalId);
}
