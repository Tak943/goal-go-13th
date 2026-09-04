package com.example.backend.entity;

import java.time.LocalDate;

import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.Column;
import jakarta.persistence.UniqueConstraint;

@Entity
@Table(name = "marks", uniqueConstraints = {
        @UniqueConstraint(columnNames = { "goal_id", "marked_date" })
})
public class Mark {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "goal_id", nullable = false, updatable = false)
    private Long goalId;

    @Column(name = "marked_date", nullable = false, updatable = false)
    private LocalDate markedDate;

    public Long getId() {
        return id;
    }

    public Long getGoalId() {
        return goalId;
    }

    public void setGoalId(Long goalId) {
        this.goalId = goalId;
    }

    public LocalDate getMarkedDate() {
        return markedDate;
    }

    public void setMarkedDate(LocalDate markedDate) {
        this.markedDate = markedDate;
    }

}
