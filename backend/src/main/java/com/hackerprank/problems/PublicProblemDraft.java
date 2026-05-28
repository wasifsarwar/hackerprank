package com.hackerprank.problems;

import java.time.Instant;

public class PublicProblemDraft {
    private final String id;
    private final String topic;
    private final String difficulty;
    private final String validationStatus;
    private final Instant createdAt;
    private final PublicGenerationMetadata generationMetadata;
    private final PublicProblem problem;

    public PublicProblemDraft(
        String id,
        String topic,
        String difficulty,
        String validationStatus,
        Instant createdAt,
        PublicGenerationMetadata generationMetadata,
        PublicProblem problem
    ) {
        this.id = id;
        this.topic = topic;
        this.difficulty = difficulty;
        this.validationStatus = validationStatus;
        this.createdAt = createdAt;
        this.generationMetadata = generationMetadata;
        this.problem = problem;
    }

    public static PublicProblemDraft from(ProblemDraft draft) {
        return new PublicProblemDraft(
            draft.getId(),
            draft.getTopic(),
            draft.getDifficulty(),
            draft.getValidationStatus(),
            draft.getCreatedAt(),
            PublicGenerationMetadata.from(draft.getGenerationMetadata()),
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

    public PublicProblem getProblem() {
        return problem;
    }
}
