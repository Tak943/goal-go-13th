package com.example.backend.entity;

import java.time.LocalDate;
import java.time.LocalTime;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;

@Entity
@Table(name = "todos")
public class Todo {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "goal_id", nullable = false, updatable = false)
    private Long goalId;

    @Column(name = "title", nullable = false)
    private String title;

    @Column(name = "time", nullable = false)
    private LocalTime time;

    @Column(name = "is_done")
    private Boolean isDone = false; // nullを扱えるためにプリミティブのbooleanじゃなくて、オブジェクトのBoolean。オブジェクトだから値をチェックしたりエンティティにセットしたりする前に、必ず
                                    // null チェックを行う

    @Column(name = "done_date")
    private LocalDate doneDate;

    public Long getId() {
        return this.id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Long getGoalId() {
        return this.goalId;
    }

    public void setGoalId(Long goalId) {
        this.goalId = goalId;
    }

    public String getTitle() {
        return this.title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public LocalTime getTime() {
        return this.time;
    }

    public void setTime(LocalTime time) {
        this.time = time;
    }

    public Boolean getIsDone() {
        return this.isDone;
    }

    public void setIsDone(boolean isDone) {
        this.isDone = isDone;
    }

    public LocalDate getDoneDate() {
        return this.doneDate;
    }

    public void setDoneDate(LocalDate doneDate) {
        this.doneDate = doneDate;
    }
}
