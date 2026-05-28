package com.hackerprank.submissions;

import java.util.List;

public class TutorHintResponse {
    private final String submissionId;
    private final String status;
    private final String provider;
    private final String level;
    private final String summary;
    private final List<String> hints;
    private final String nextStep;

    public TutorHintResponse(
        String submissionId,
        String status,
        String provider,
        String level,
        String summary,
        List<String> hints,
        String nextStep
    ) {
        this.submissionId = submissionId;
        this.status = status;
        this.provider = provider;
        this.level = level;
        this.summary = summary;
        this.hints = List.copyOf(hints);
        this.nextStep = nextStep;
    }

    public String getSubmissionId() {
        return submissionId;
    }

    public String getStatus() {
        return status;
    }

    public String getProvider() {
        return provider;
    }

    public String getLevel() {
        return level;
    }

    public String getSummary() {
        return summary;
    }

    public List<String> getHints() {
        return hints;
    }

    public String getNextStep() {
        return nextStep;
    }
}
