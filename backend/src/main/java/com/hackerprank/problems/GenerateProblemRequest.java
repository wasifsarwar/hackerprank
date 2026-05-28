package com.hackerprank.problems;

import java.util.List;

public class GenerateProblemRequest {
    private String topic;
    private String difficulty;
    private List<String> targetConcepts;
    private String constraintsNotes;
    private String interviewStyle;

    public String getTopic() {
        return topic;
    }

    public void setTopic(String topic) {
        this.topic = topic;
    }

    public String getDifficulty() {
        return difficulty;
    }

    public void setDifficulty(String difficulty) {
        this.difficulty = difficulty;
    }

    public List<String> getTargetConcepts() {
        return targetConcepts;
    }

    public void setTargetConcepts(List<String> targetConcepts) {
        this.targetConcepts = targetConcepts;
    }

    public String getConstraintsNotes() {
        return constraintsNotes;
    }

    public void setConstraintsNotes(String constraintsNotes) {
        this.constraintsNotes = constraintsNotes;
    }

    public String getInterviewStyle() {
        return interviewStyle;
    }

    public void setInterviewStyle(String interviewStyle) {
        this.interviewStyle = interviewStyle;
    }
}
