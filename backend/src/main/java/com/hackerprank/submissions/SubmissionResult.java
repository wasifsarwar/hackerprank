package com.hackerprank.submissions;

import java.time.Instant;
import java.util.List;

public class SubmissionResult {
    private final String submissionId;
    private final Instant createdAt;
    private final String status;
    private final int passedCount;
    private final int totalCount;
    private final String compileOutput;
    private final List<TestCaseResult> results;

    public SubmissionResult(
        String status,
        int passedCount,
        int totalCount,
        String compileOutput,
        List<TestCaseResult> results
    ) {
        this(null, null, status, passedCount, totalCount, compileOutput, results);
    }

    public SubmissionResult(
        String submissionId,
        Instant createdAt,
        String status,
        int passedCount,
        int totalCount,
        String compileOutput,
        List<TestCaseResult> results
    ) {
        this.submissionId = submissionId;
        this.createdAt = createdAt;
        this.status = status;
        this.passedCount = passedCount;
        this.totalCount = totalCount;
        this.compileOutput = compileOutput;
        this.results = results;
    }

    public SubmissionResult withPersistence(String nextSubmissionId, Instant nextCreatedAt) {
        return new SubmissionResult(
            nextSubmissionId,
            nextCreatedAt,
            status,
            passedCount,
            totalCount,
            compileOutput,
            results
        );
    }

    public String getSubmissionId() {
        return submissionId;
    }

    public Instant getCreatedAt() {
        return createdAt;
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

    public List<TestCaseResult> getResults() {
        return results;
    }
}
