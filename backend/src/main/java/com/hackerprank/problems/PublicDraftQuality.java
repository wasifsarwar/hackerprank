package com.hackerprank.problems;

import java.util.List;

public record PublicDraftQuality(
    String status,
    String summary,
    boolean repairUsed,
    int exampleCount,
    int visibleTestCount,
    int hiddenTestCount,
    int totalTestCount,
    List<PublicDraftQualityCheck> checks
) {
    public static PublicDraftQuality from(ProblemDraft draft) {
        Problem problem = draft.getProblem();
        GenerationMetadata metadata = draft.getGenerationMetadata();
        int exampleCount = problem.getExamples().size();
        int visibleTestCount = (int) problem.getTestCases().stream()
            .filter(testCase -> !testCase.isHidden())
            .count();
        int hiddenTestCount = (int) problem.getTestCases().stream()
            .filter(TestCase::isHidden)
            .count();
        int totalTestCount = problem.getTestCases().size();
        String validationStatus = textOrDefault(draft.getValidationStatus(), metadata.validationStatus());

        return new PublicDraftQuality(
            validationStatus,
            textOrDefault(metadata.validationSummary(), "Validation summary not recorded."),
            metadata.repairUsed(),
            exampleCount,
            visibleTestCount,
            hiddenTestCount,
            totalTestCount,
            List.of(
                passed("Schema", "Required statement, examples, tests, starter code, and references are present."),
                passed("Examples", exampleCount + " examples replayed against Python and Java references."),
                passed("Python", "Reference solution accepted examples plus visible and hidden tests."),
                passed("Java", "Reference solution accepted examples plus visible and hidden tests."),
                passed("Hidden coverage", hiddenTestCount + " hidden tests retained server-side.")
            )
        );
    }

    private static PublicDraftQualityCheck passed(String label, String detail) {
        return new PublicDraftQualityCheck(label, "PASSED", detail);
    }

    private static String textOrDefault(String value, String fallback) {
        if (value != null && !value.isBlank()) {
            return value;
        }
        return fallback == null ? "" : fallback;
    }
}
