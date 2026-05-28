package com.hackerprank.submissions;

import java.time.Instant;
import java.util.List;

public class SubmissionDetail {
    private final String id;
    private final String problemId;
    private final String problemTitle;
    private final String problemDifficulty;
    private final String language;
    private final String code;
    private final boolean runHiddenTests;
    private final String status;
    private final int passedCount;
    private final int totalCount;
    private final String compileOutput;
    private final Instant createdAt;
    private final List<TestCaseResult> results;

    public SubmissionDetail(
        String id,
        String problemId,
        String problemTitle,
        String problemDifficulty,
        String language,
        String code,
        boolean runHiddenTests,
        String status,
        int passedCount,
        int totalCount,
        String compileOutput,
        Instant createdAt,
        List<TestCaseResult> results
    ) {
        this.id = id;
        this.problemId = problemId;
        this.problemTitle = problemTitle;
        this.problemDifficulty = problemDifficulty;
        this.language = language;
        this.code = code;
        this.runHiddenTests = runHiddenTests;
        this.status = status;
        this.passedCount = passedCount;
        this.totalCount = totalCount;
        this.compileOutput = compileOutput;
        this.createdAt = createdAt;
        this.results = results;
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

    public String getCode() {
        return code;
    }

    public boolean isRunHiddenTests() {
        return runHiddenTests;
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

    public String getCompileOutput() {
        return compileOutput;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public List<TestCaseResult> getResults() {
        return results;
    }
}
