package com.hackerprank.submissions;

import java.util.List;

public class TutorChatResponse {
    private final String submissionId;
    private final List<TutorMessage> messages;

    public TutorChatResponse(String submissionId, List<TutorMessage> messages) {
        this.submissionId = submissionId;
        this.messages = List.copyOf(messages);
    }

    public String getSubmissionId() {
        return submissionId;
    }

    public List<TutorMessage> getMessages() {
        return messages;
    }
}
