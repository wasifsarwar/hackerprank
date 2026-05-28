package com.hackerprank.problems;

import java.time.Instant;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;

public class ProblemDraft {
    private final String id;
    private final String topic;
    private final String difficulty;
    private final Problem problem;
    private final Map<String, String> referenceSolutions;
    private final String validationStatus;
    private final GenerationMetadata generationMetadata;
    private final Instant createdAt;

    public ProblemDraft(
        String id,
        String topic,
        String difficulty,
        Problem problem,
        Map<String, String> referenceSolutions,
        String validationStatus,
        GenerationMetadata generationMetadata,
        Instant createdAt
    ) {
        this.id = id;
        this.topic = topic;
        this.difficulty = difficulty;
        this.problem = problem;
        this.referenceSolutions = referenceSolutions == null
            ? Map.of()
            : Collections.unmodifiableMap(new LinkedHashMap<>(referenceSolutions));
        this.validationStatus = validationStatus;
        this.generationMetadata = generationMetadata == null ? GenerationMetadata.empty() : generationMetadata;
        this.createdAt = createdAt;
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

    public Problem getProblem() {
        return problem;
    }

    public Map<String, String> getReferenceSolutions() {
        return referenceSolutions;
    }

    public String getReferenceSolution() {
        return referenceSolutions.getOrDefault("python", "");
    }

    public String getValidationStatus() {
        return validationStatus;
    }

    public GenerationMetadata getGenerationMetadata() {
        return generationMetadata;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }
}
