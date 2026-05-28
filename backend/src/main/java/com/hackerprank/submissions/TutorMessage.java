package com.hackerprank.submissions;

import java.time.Instant;

public class TutorMessage {
    private final String id;
    private final String submissionId;
    private final String role;
    private final String provider;
    private final String content;
    private final Instant createdAt;

    public TutorMessage(String id, String submissionId, String role, String provider, String content, Instant createdAt) {
        this.id = id;
        this.submissionId = submissionId;
        this.role = role;
        this.provider = provider;
        this.content = content;
        this.createdAt = createdAt;
    }

    public String getId() {
        return id;
    }

    public String getSubmissionId() {
        return submissionId;
    }

    public String getRole() {
        return role;
    }

    public String getProvider() {
        return provider;
    }

    public String getContent() {
        return content;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }
}
