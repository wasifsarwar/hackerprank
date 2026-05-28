package com.hackerprank.submissions;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.time.Instant;
import java.util.List;

import org.junit.jupiter.api.Test;

class TutorHintServiceTests {
    private final TutorHintService tutorHintService = new TutorHintService();

    @Test
    void includesVisibleFailureDiffForWrongAnswers() {
        SubmissionDetail submission = submission(
            "WRONG_ANSWER",
            List.of(new TestCaseResult("sample one", false, false, "4\n", "5\n", "", false, 0, 25))
        );

        TutorHintResponse hint = tutorHintService.createHint(submission);
        String joinedHints = String.join("\n", hint.getHints());

        assertEquals("deterministic", hint.getProvider());
        assertEquals("nudge", hint.getLevel());
        assertTrue(hint.getSummary().contains("visible test"));
        assertTrue(joinedHints.contains("expected `4`"));
        assertTrue(joinedHints.contains("got `5`"));
    }

    @Test
    void keepsHiddenOnlyFailuresGeneric() {
        SubmissionDetail submission = submission(
            "WRONG_ANSWER",
            List.of(new TestCaseResult("hidden boundary", true, false, null, "secret actual output", "", false, 0, 31))
        );

        TutorHintResponse hint = tutorHintService.createHint(submission);
        String joined = hint.getSummary() + "\n" + String.join("\n", hint.getHints()) + "\n" + hint.getNextStep();

        assertTrue(joined.contains("hidden"));
        assertFalse(joined.contains("secret actual output"));
        assertFalse(joined.contains("hidden boundary"));
    }

    @Test
    void usesCompileOutputForCompileErrors() {
        SubmissionDetail submission = new SubmissionDetail(
            "submission-1",
            "problem-1",
            "Problem",
            "Easy",
            "java",
            "public class Main {}",
            false,
            "COMPILE_ERROR",
            0,
            2,
            "Main.java:3: error: cannot find symbol",
            Instant.EPOCH,
            List.of()
        );

        TutorHintResponse hint = tutorHintService.createHint(submission);
        String joinedHints = String.join("\n", hint.getHints());

        assertEquals("COMPILE_ERROR", hint.getStatus());
        assertTrue(joinedHints.contains("symbol"));
        assertTrue(joinedHints.contains("First diagnostic"));
    }

    private SubmissionDetail submission(String status, List<TestCaseResult> results) {
        return new SubmissionDetail(
            "submission-1",
            "problem-1",
            "Problem",
            "Easy",
            "python",
            "print(0)",
            true,
            status,
            0,
            results.size(),
            "",
            Instant.EPOCH,
            results
        );
    }
}
