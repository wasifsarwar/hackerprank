package com.hackerprank.problems;

import java.util.List;

public class ProblemSummary {
    private final String id;
    private final String title;
    private final String difficulty;
    private final List<String> tags;

    public ProblemSummary(String id, String title, String difficulty, List<String> tags) {
        this.id = id;
        this.title = title;
        this.difficulty = difficulty;
        this.tags = tags;
    }

    public static ProblemSummary from(Problem problem) {
        return new ProblemSummary(problem.getId(), problem.getTitle(), problem.getDifficulty(), problem.getTags());
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
}
