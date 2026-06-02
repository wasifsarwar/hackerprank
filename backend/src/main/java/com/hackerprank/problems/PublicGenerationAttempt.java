package com.hackerprank.problems;

import java.time.Instant;
import java.util.List;

public record PublicGenerationAttempt(
    String id,
    String outcome,
    List<String> feedbackTags,
    String feedbackNotes,
    String promptHash,
    String responseHash,
    int promptCharCount,
    int responseCharCount,
    int estimatedPromptTokens,
    int estimatedResponseTokens,
    Instant createdAt,
    Instant updatedAt
) {
    public static PublicGenerationAttempt from(GenerationAttempt attempt) {
        if (attempt == null) {
            return null;
        }

        return new PublicGenerationAttempt(
            attempt.getId(),
            attempt.getOutcome(),
            attempt.getFeedbackTags(),
            attempt.getFeedbackNotes(),
            attempt.getUsageMetrics().promptHash(),
            attempt.getUsageMetrics().responseHash(),
            attempt.getUsageMetrics().promptCharCount(),
            attempt.getUsageMetrics().responseCharCount(),
            attempt.getUsageMetrics().estimatedPromptTokens(),
            attempt.getUsageMetrics().estimatedResponseTokens(),
            attempt.getCreatedAt(),
            attempt.getUpdatedAt()
        );
    }
}
