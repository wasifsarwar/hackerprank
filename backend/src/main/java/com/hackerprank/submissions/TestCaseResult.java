package com.hackerprank.submissions;

public class TestCaseResult {
    private final String name;
    private final boolean hidden;
    private final boolean passed;
    private final String expectedOutput;
    private final String actualOutput;
    private final String stderr;
    private final boolean timedOut;
    private final long runtimeMs;

    public TestCaseResult(
        String name,
        boolean hidden,
        boolean passed,
        String expectedOutput,
        String actualOutput,
        String stderr,
        boolean timedOut,
        long runtimeMs
    ) {
        this.name = name;
        this.hidden = hidden;
        this.passed = passed;
        this.expectedOutput = expectedOutput;
        this.actualOutput = actualOutput;
        this.stderr = stderr;
        this.timedOut = timedOut;
        this.runtimeMs = runtimeMs;
    }

    public String getName() {
        return name;
    }

    public boolean isHidden() {
        return hidden;
    }

    public boolean isPassed() {
        return passed;
    }

    public String getExpectedOutput() {
        return expectedOutput;
    }

    public String getActualOutput() {
        return actualOutput;
    }

    public String getStderr() {
        return stderr;
    }

    public boolean isTimedOut() {
        return timedOut;
    }

    public long getRuntimeMs() {
        return runtimeMs;
    }
}
