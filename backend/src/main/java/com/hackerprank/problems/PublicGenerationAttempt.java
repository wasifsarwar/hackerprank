package com.hackerprank.problems;

import java.time.Instant;
import java.util.List;

public record PublicGenerationAttempt(
    String id,
    String outcome,
    List<String> feedbackTags,
    String feedbackNotes,
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
            attempt.getCreatedAt(),
            attempt.getUpdatedAt()
        );
    }
}
