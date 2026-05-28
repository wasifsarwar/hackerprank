package com.hackerprank.problems;

import java.time.Instant;

public class ProblemDraft {
    private final String id;
    private final String topic;
    private final String difficulty;
    private final Problem problem;
    private final String referenceSolution;
    private final String validationStatus;
    private final Instant createdAt;

    public ProblemDraft(
        String id,
        String topic,
        String difficulty,
        Problem problem,
        String referenceSolution,
        String validationStatus,
        Instant createdAt
    ) {
        this.id = id;
        this.topic = topic;
        this.difficulty = difficulty;
        this.problem = problem;
        this.referenceSolution = referenceSolution;
        this.validationStatus = validationStatus;
        this.createdAt = createdAt;
    }

    public String getId() {
        return id;
    }

    public String getTopic() {
        return topic;
    }

    public String getDifficulty() {
        return difficulty;
    }

    public Problem getProblem() {
        return problem;
    }

    public String getReferenceSolution() {
        return referenceSolution;
    }

    public String getValidationStatus() {
        return validationStatus;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }
}

