package com.hackerprank.submissions;

class ProcessResult {
    private final int exitCode;
    private final String stdout;
    private final String stderr;
    private final boolean timedOut;
    private final long runtimeMs;

    ProcessResult(int exitCode, String stdout, String stderr, boolean timedOut, long runtimeMs) {
        this.exitCode = exitCode;
        this.stdout = stdout;
        this.stderr = stderr;
        this.timedOut = timedOut;
        this.runtimeMs = runtimeMs;
    }

    int getExitCode() {
        return exitCode;
    }

    String getStdout() {
        return stdout;
    }

    String getStderr() {
        return stderr;
    }

    boolean isTimedOut() {
        return timedOut;
    }

    long getRuntimeMs() {
        return runtimeMs;
    }
}
