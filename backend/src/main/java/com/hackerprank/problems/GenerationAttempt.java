package com.hackerprank.problems;

import java.time.Instant;
import java.util.List;

public class GenerationAttempt {
    private final String id;
    private final String draftId;
    private final String problemId;
    private final String provider;
    private final String modelId;
    private final String promptVersion;
    private final String topic;
    private final String difficulty;
    private final String outcome;
    private final List<String> feedbackTags;
    private final String feedbackNotes;
    private final GenerationUsageMetrics usageMetrics;
    private final Instant createdAt;
    private final Instant updatedAt;

    public GenerationAttempt(
        String id,
        String draftId,
        String problemId,
        String provider,
        String modelId,
        String promptVersion,
        String topic,
        String difficulty,
        String outcome,
        List<String> feedbackTags,
        String feedbackNotes,
        GenerationUsageMetrics usageMetrics,
        Instant createdAt,
        Instant updatedAt
    ) {
        this.id = id;
        this.draftId = draftId;
        this.problemId = problemId;
        this.provider = provider;
        this.modelId = modelId;
        this.promptVersion = promptVersion;
        this.topic = topic;
        this.difficulty = difficulty;
        this.outcome = outcome;
        this.feedbackTags = feedbackTags == null ? List.of() : List.copyOf(feedbackTags);
        this.feedbackNotes = feedbackNotes == null ? "" : feedbackNotes;
        this.usageMetrics = usageMetrics == null ? GenerationUsageMetrics.empty() : usageMetrics;
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
    }

    public String getId() {
        return id;
    }

    public String getDraftId() {
        return draftId;
    }

    public String getProblemId() {
        return problemId;
    }

    public String getProvider() {
        return provider;
    }

    public String getModelId() {
        return modelId;
    }

    public String getPromptVersion() {
        return promptVersion;
    }

    public String getTopic() {
        return topic;
    }

    public String getDifficulty() {
        return difficulty;
    }

    public String getOutcome() {
        return outcome;
    }

    public List<String> getFeedbackTags() {
        return feedbackTags;
    }

    public String getFeedbackNotes() {
        return feedbackNotes;
    }

    public GenerationUsageMetrics getUsageMetrics() {
        return usageMetrics;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public Instant getUpdatedAt() {
        return updatedAt;
    }
}
