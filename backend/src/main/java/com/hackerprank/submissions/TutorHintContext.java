package com.hackerprank.submissions;

import com.hackerprank.problems.PublicProblem;

import java.util.List;

record TutorHintContext(
    SubmissionDetail submission,
    PublicProblem problem,
    List<VisibleFailure> visibleFailures,
    int hiddenTestCount,
    int hiddenFailureCount,
    int hiddenTimedOutCount
) {
    static TutorHintContext from(SubmissionDetail submission, PublicProblem problem) {
        List<VisibleFailure> visibleFailures = submission.getResults().stream()
            .filter(result -> !result.isPassed())
            .filter(result -> !result.isHidden())
            .map(result -> new VisibleFailure(
                result.getName(),
                result.getExpectedOutput(),
                result.getActualOutput(),
                result.getStderr(),
                result.isTimedOut(),
                result.getExitCode()
            ))
            .toList();

        int hiddenTestCount = (int) submission.getResults().stream()
            .filter(TestCaseResult::isHidden)
            .count();
        int hiddenFailureCount = (int) submission.getResults().stream()
            .filter(result -> result.isHidden() && !result.isPassed())
            .count();
        int hiddenTimedOutCount = (int) submission.getResults().stream()
            .filter(result -> result.isHidden() && !result.isPassed() && result.isTimedOut())
            .count();

        return new TutorHintContext(
            submission,
            problem,
            visibleFailures,
            hiddenTestCount,
            hiddenFailureCount,
            hiddenTimedOutCount
        );
    }

    record VisibleFailure(
        String name,
        String expectedOutput,
        String actualOutput,
        String stderr,
        boolean timedOut,
        int exitCode
    ) {
    }
}
