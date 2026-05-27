package com.hackerprank.submissions;

import java.util.List;

public class SubmissionResult {
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
        this.status = status;
        this.passedCount = passedCount;
        this.totalCount = totalCount;
        this.compileOutput = compileOutput;
        this.results = results;
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
