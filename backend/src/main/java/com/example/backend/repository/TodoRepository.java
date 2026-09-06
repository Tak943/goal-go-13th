package com.example.backend.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

import com.example.backend.entity.Todo;

public interface TodoRepository extends JpaRepository<Todo, Long>{
    List<Todo> findByGoalId(Long goalId);
    List<Todo> findByGoalIdAndIsDone(Long goalId, Boolean isDone);
}
