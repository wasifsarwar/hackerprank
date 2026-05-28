package com.hackerprank.submissions;

import java.util.List;
import java.util.Locale;

import org.springframework.stereotype.Service;

@Service
class TutorChatService {
    private static final int MAX_USER_MESSAGE_LENGTH = 1200;

    private final TutorProperties properties;
    private final TutorSubmissionContextFactory contextFactory;
    private final TutorMessageRepository messageRepository;
    private final OpenAiTutorChatGenerator openAiTutorChatGenerator;

    TutorChatService(
        TutorProperties properties,
        TutorSubmissionContextFactory contextFactory,
        TutorMessageRepository messageRepository,
        OpenAiTutorChatGenerator openAiTutorChatGenerator
    ) {
        this.properties = properties;
        this.contextFactory = contextFactory;
        this.messageRepository = messageRepository;
        this.openAiTutorChatGenerator = openAiTutorChatGenerator;
    }

    List<TutorMessage> findMessages(String submissionId) {
        return messageRepository.findBySubmissionId(submissionId);
    }

    TutorChatResponse createReply(SubmissionDetail submission, TutorMessageRequest request) {
        String userMessage = normalizeMessage(request == null ? "" : request.message());
        messageRepository.save(submission.getId(), "user", "user", userMessage);

        List<TutorMessage> recentMessages = messageRepository.findBySubmissionId(submission.getId());
        TutorChatReply reply = createAssistantReply(submission, recentMessages, userMessage);
        messageRepository.save(submission.getId(), "assistant", reply.provider(), reply.content());

        return new TutorChatResponse(submission.getId(), messageRepository.findBySubmissionId(submission.getId()));
    }

    private TutorChatReply createAssistantReply(
        SubmissionDetail submission,
        List<TutorMessage> recentMessages,
        String userMessage
    ) {
        if (shouldUseOpenAi(submission)) {
            try {
                return openAiTutorChatGenerator.createReply(contextFactory.create(submission), recentMessages);
            } catch (OpenAiTutorHintException exception) {
                return deterministicReply(submission, userMessage);
            }
        }

        return deterministicReply(submission, userMessage);
    }

    private boolean shouldUseOpenAi(SubmissionDetail submission) {
        return properties.prefersOpenAi()
            && openAiTutorChatGenerator != null
            && openAiTutorChatGenerator.isConfigured()
            && !"ACCEPTED".equals(normalizeStatus(submission.getStatus()));
    }

    private TutorChatReply deterministicReply(SubmissionDetail submission, String userMessage) {
        String lowerMessage = userMessage.toLowerCase(Locale.ROOT);
        TestCaseResult visibleFailure = firstVisibleFailure(submission);

        if (lowerMessage.contains("complex") || lowerMessage.contains("big o") || lowerMessage.contains("runtime")) {
            return new TutorChatReply(
                "deterministic",
                "Anchor the complexity estimate to the largest constraint, then count how many times each input item can be revisited. If any value can trigger a full scan, look for a map, prefix structure, heap, stack, or sorted sweep that makes that work one-time."
            );
        }

        if (visibleFailure != null) {
            return new TutorChatReply(
                "deterministic",
                "Use the first visible mismatch as the debugging boundary. Trace the sample by hand, write down each state change, and compare it to your code's state at the same step. The smallest branch where those states diverge is the next thing to fix."
            );
        }

        if (hasHiddenFailure(submission)) {
            return new TutorChatReply(
                "deterministic",
                "The visible cases are not enough to isolate it, so invent one edge case at a time. Start with minimum input, duplicate values, ties, negative or zero values if allowed, and the largest shape from the constraints. Keep hidden details private and reason from categories."
            );
        }

        return new TutorChatReply(
            "deterministic",
            "Start by rerunning samples and reading the first concrete signal: compiler output, visible mismatch, timeout, or crash. Make one small change that explains that signal before changing the broader approach."
        );
    }

    private String normalizeMessage(String message) {
        String normalized = message == null ? "" : message.replace("\r\n", "\n").trim();
        if (normalized.isBlank()) {
            throw new IllegalArgumentException("Tutor message is required");
        }
        if (normalized.length() > MAX_USER_MESSAGE_LENGTH) {
            throw new IllegalArgumentException("Tutor message must be " + MAX_USER_MESSAGE_LENGTH + " characters or fewer");
        }
        return normalized;
    }

    private TestCaseResult firstVisibleFailure(SubmissionDetail submission) {
        return submission.getResults().stream()
            .filter(result -> !result.isPassed())
            .filter(result -> !result.isHidden())
            .findFirst()
            .orElse(null);
    }

    private boolean hasHiddenFailure(SubmissionDetail submission) {
        return submission.getResults().stream()
            .anyMatch(result -> !result.isPassed() && result.isHidden());
    }

    private String normalizeStatus(String status) {
        return status == null ? "WRONG_ANSWER" : status.toUpperCase(Locale.ROOT);
    }
}
