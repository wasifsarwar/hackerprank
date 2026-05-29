package com.hackerprank.problems;

import java.util.List;

public class DraftFeedbackRequest {
    private List<String> tags;
    private String notes;

    public List<String> getTags() {
        return tags;
    }

    public void setTags(List<String> tags) {
        this.tags = tags;
    }

    public String getNotes() {
        return notes;
    }

    public void setNotes(String notes) {
        this.notes = notes;
    }
}
