package com.hackerprank.submissions;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

import org.springframework.stereotype.Service;

@Service
public class TutorHintService {
    private static final String PROVIDER = "deterministic";
    private static final String LEVEL = "nudge";
    private static final int MAX_SNIPPET_LENGTH = 240;

    public TutorHintResponse createHint(SubmissionDetail submission) {
        String status = normalizeStatus(submission.getStatus());
        return switch (status) {
            case "ACCEPTED" -> acceptedHint(submission);
            case "COMPILE_ERROR" -> compileHint(submission);
            case "TIME_LIMIT_EXCEEDED" -> timeoutHint(submission);
            case "RUNTIME_ERROR" -> runtimeHint(submission);
            default -> wrongAnswerHint(submission);
        };
    }

    private TutorHintResponse acceptedHint(SubmissionDetail submission) {
        return response(
            submission,
            "Accepted. The next learning move is to pressure-test your reasoning.",
            List.of(
                "Write down the invariant your code maintains after each input item is processed.",
                "Try explaining why the slowest test case still fits inside the expected complexity."
            ),
            "Re-solve it in the other language or tighten the explanation until every branch feels necessary."
        );
    }

    private TutorHintResponse compileHint(SubmissionDetail submission) {
        List<String> hints = new ArrayList<>();
        String compileOutput = compact(submission.getCompileOutput());
        String lowerOutput = compileOutput.toLowerCase(Locale.ROOT);

        if (lowerOutput.contains("cannot find symbol") || lowerOutput.contains("nameerror")) {
            hints.add("A symbol or name is missing. Check spelling, scope, and whether the value is initialized before use.");
        } else if (lowerOutput.contains("class main") || lowerOutput.contains("public class")) {
            hints.add("For Java submissions, keep the entry point in `public class Main` with a `main` method.");
        } else {
            hints.add("Start with the first compiler diagnostic, then re-run before chasing later errors.");
        }

        if (!compileOutput.isBlank()) {
            hints.add("First diagnostic: " + compileOutput);
        }

        return response(
            submission,
            "The code did not compile, so the tests never started.",
            hints,
            "Fix the first compile error only, then run samples again before changing algorithm logic."
        );
    }

    private TutorHintResponse timeoutHint(SubmissionDetail submission) {
        return response(
            submission,
            "At least one test timed out, which usually means the algorithm is doing too much repeated work.",
            List.of(
                "Estimate the worst-case operation count from the constraints, not from the sample input size.",
                "Look for a repeated scan, nested loop, or recomputation that can become a one-pass map, stack, or prefix structure."
            ),
            "Create the largest input shape allowed by the constraints and trace how many times your inner work repeats."
        );
    }

    private TutorHintResponse runtimeHint(SubmissionDetail submission) {
        List<String> hints = new ArrayList<>();
        TestCaseResult visibleFailure = firstVisibleFailure(submission);
        if (visibleFailure != null) {
            String stderr = compact(visibleFailure.getStderr());
            if (!stderr.isBlank()) {
                hints.add("The visible failing test exits with: " + stderr);
            }
            hints.add("Check index bounds, empty input handling, parsing assumptions, and null or missing values around that test.");
        } else {
            hints.add("A failing case crashes before producing a valid answer. Hidden-case details stay private, so focus on boundary inputs.");
            hints.add("Check minimum-size input, maximum-size input, duplicate values, and parsing when lines contain extra spaces.");
        }

        return response(
            submission,
            "The program crashed during execution.",
            hints,
            "Add one tiny guard or assertion around the suspected boundary, then run samples again."
        );
    }

    private TutorHintResponse wrongAnswerHint(SubmissionDetail submission) {
        List<String> hints = new ArrayList<>();
        TestCaseResult visibleFailure = firstVisibleFailure(submission);
        if (visibleFailure != null) {
            addVisibleDiffHint(hints, visibleFailure);
            hints.add("Trace your variables for that visible test and stop at the first point where your state diverges from the expected result.");
            return response(
                submission,
                "A visible test is failing, so start by reconciling that input/output pair.",
                hints,
                "Patch the smallest logic branch that explains the visible mismatch, then run hidden tests after samples pass."
            );
        }

        if (hasHiddenFailure(submission)) {
            hints.add("Samples passed, so the bug is likely in an edge case the examples do not cover.");
            hints.add("Audit ties, duplicates, negative values, empty or minimum-size input, and the largest allowed input.");
            return response(
                submission,
                "A hidden case is still failing.",
                hints,
                "Invent one new edge-case sample and predict the answer by hand before changing code."
            );
        }

        hints.add("No failing test detail was captured. Re-run samples first to get a smaller signal.");
        return response(
            submission,
            "The answer was not accepted, but there is not enough per-test detail yet.",
            hints,
            "Run samples and use the first visible mismatch as the debugging anchor."
        );
    }

    private void addVisibleDiffHint(List<String> hints, TestCaseResult failure) {
        String expected = compact(failure.getExpectedOutput());
        String actual = compact(failure.getActualOutput());
        if (!expected.isBlank() && !actual.isBlank()) {
            hints.add("On visible test `" + failure.getName() + "`, expected `" + expected + "` but got `" + actual + "`.");
            return;
        }

        if (!expected.isBlank()) {
            hints.add("On visible test `" + failure.getName() + "`, expected `" + expected + "`, but your output was empty.");
            return;
        }

        hints.add("Use the first visible failing test as the anchor and compare each intermediate value with the expected path.");
    }

    private TutorHintResponse response(SubmissionDetail submission, String summary, List<String> hints, String nextStep) {
        return new TutorHintResponse(
            submission.getId(),
            normalizeStatus(submission.getStatus()),
            PROVIDER,
            LEVEL,
            summary,
            hints,
            nextStep
        );
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

    private String compact(String value) {
        if (value == null) {
            return "";
        }

        String compacted = value.replace("\r\n", "\n").trim();
        if (compacted.length() <= MAX_SNIPPET_LENGTH) {
            return compacted;
        }
        return compacted.substring(0, MAX_SNIPPET_LENGTH) + "...";
    }
}
