package com.hackerprank.submissions;

public class SubmissionRequest {
    private String problemId;
    private String language;
    private String code;
    private boolean runHiddenTests;

    public String getProblemId() {
        return problemId;
    }

    public void setProblemId(String problemId) {
        this.problemId = problemId;
    }

    public String getLanguage() {
        return language;
    }

    public void setLanguage(String language) {
        this.language = language;
    }

    public String getCode() {
        return code;
    }

    public void setCode(String code) {
        this.code = code;
    }

    public boolean isRunHiddenTests() {
        return runHiddenTests;
    }

    public void setRunHiddenTests(boolean runHiddenTests) {
        this.runHiddenTests = runHiddenTests;
    }
}
