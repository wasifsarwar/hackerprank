package com.hackerprank.problems;

import java.time.Instant;

public class PublicProblemDraft {
    private final String id;
    private final String topic;
    private final String difficulty;
    private final String validationStatus;
    private final Instant createdAt;
    private final PublicGenerationMetadata generationMetadata;
    private final PublicDraftQuality quality;
    private final PublicGenerationAttempt generationAttempt;
    private final PublicProblem problem;

    public PublicProblemDraft(
        String id,
        String topic,
        String difficulty,
        String validationStatus,
        Instant createdAt,
        PublicGenerationMetadata generationMetadata,
        PublicDraftQuality quality,
        PublicGenerationAttempt generationAttempt,
        PublicProblem problem
    ) {
        this.id = id;
        this.topic = topic;
        this.difficulty = difficulty;
        this.validationStatus = validationStatus;
        this.createdAt = createdAt;
        this.generationMetadata = generationMetadata;
        this.quality = quality;
        this.generationAttempt = generationAttempt;
        this.problem = problem;
    }

    public static PublicProblemDraft from(ProblemDraft draft) {
        return from(draft, null);
    }

    public static PublicProblemDraft from(ProblemDraft draft, GenerationAttempt attempt) {
        return new PublicProblemDraft(
            draft.getId(),
            draft.getTopic(),
            draft.getDifficulty(),
            draft.getValidationStatus(),
            draft.getCreatedAt(),
            PublicGenerationMetadata.from(draft.getGenerationMetadata()),
            PublicDraftQuality.from(draft),
            PublicGenerationAttempt.from(attempt),
            PublicProblem.from(draft.getProblem())
        );
    }

    public String getId() {
        return id;
    }

    public String getTopic() {
        return topic;
    }

    public String getDifficulty() {
        return difficulty;
    }

    public String getValidationStatus() {
        return validationStatus;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public PublicGenerationMetadata getGenerationMetadata() {
        return generationMetadata;
    }

    public PublicDraftQuality getQuality() {
        return quality;
    }

    public PublicGenerationAttempt getGenerationAttempt() {
        return generationAttempt;
    }

    public PublicProblem getProblem() {
        return problem;
    }
}
