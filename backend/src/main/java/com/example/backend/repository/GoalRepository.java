package com.example.backend.repository;

import org.springframework.data.jpa.repository.JpaRepository;

import com.example.backend.entity.Goal;

public interface GoalRepository extends JpaRepository<Goal, Long>{

}
