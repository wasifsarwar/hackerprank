package com.hackerprank.submissions;

import java.time.Instant;

public class SubmissionSummary {
    private final String id;
    private final String problemId;
    private final String problemTitle;
    private final String problemDifficulty;
    private final String language;
    private final String status;
    private final int passedCount;
    private final int totalCount;
    private final Instant createdAt;

    public SubmissionSummary(
        String id,
        String problemId,
        String problemTitle,
        String problemDifficulty,
        String language,
        String status,
        int passedCount,
        int totalCount,
        Instant createdAt
    ) {
        this.id = id;
        this.problemId = problemId;
        this.problemTitle = problemTitle;
        this.problemDifficulty = problemDifficulty;
        this.language = language;
        this.status = status;
        this.passedCount = passedCount;
        this.totalCount = totalCount;
        this.createdAt = createdAt;
    }

    public String getId() {
        return id;
    }

    public String getProblemId() {
        return problemId;
    }

    public String getProblemTitle() {
        return problemTitle;
    }

    public String getProblemDifficulty() {
        return problemDifficulty;
    }

    public String getLanguage() {
        return language;
    }

    public String getStatus() {
        return status;
    }

    public int getPassedCount() {
        return passedCount;
    }

    public int getTotalCount() {
        return totalCount;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }
}
