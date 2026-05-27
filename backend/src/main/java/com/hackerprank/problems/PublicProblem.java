package com.hackerprank.problems;

import java.util.List;
import java.util.Map;

public class PublicProblem {
    private final String id;
    private final String title;
    private final String difficulty;
    private final List<String> tags;
    private final String description;
    private final String inputFormat;
    private final String outputFormat;
    private final List<String> constraints;
    private final List<Example> examples;
    private final Map<String, String> starterCode;

    public PublicProblem(
        String id,
        String title,
        String difficulty,
        List<String> tags,
        String description,
        String inputFormat,
        String outputFormat,
        List<String> constraints,
        List<Example> examples,
        Map<String, String> starterCode
    ) {
        this.id = id;
        this.title = title;
        this.difficulty = difficulty;
        this.tags = tags;
        this.description = description;
        this.inputFormat = inputFormat;
        this.outputFormat = outputFormat;
        this.constraints = constraints;
        this.examples = examples;
        this.starterCode = starterCode;
    }

    public static PublicProblem from(Problem problem) {
        return new PublicProblem(
            problem.getId(),
            problem.getTitle(),
            problem.getDifficulty(),
            problem.getTags(),
            problem.getDescription(),
            problem.getInputFormat(),
            problem.getOutputFormat(),
            problem.getConstraints(),
            problem.getExamples(),
            problem.getStarterCode()
        );
    }

    public String getId() {
        return id;
    }

    public String getTitle() {
        return title;
    }

    public String getDifficulty() {
        return difficulty;
    }

    public List<String> getTags() {
        return tags;
    }

    public String getDescription() {
        return description;
    }

    public String getInputFormat() {
        return inputFormat;
    }

    public String getOutputFormat() {
        return outputFormat;
    }

    public List<String> getConstraints() {
        return constraints;
    }

    public List<Example> getExamples() {
        return examples;
    }

    public Map<String, String> getStarterCode() {
        return starterCode;
    }
}
