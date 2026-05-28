package com.hackerprank.submissions;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.hackerprank.problems.Example;
import com.hackerprank.problems.PublicProblem;

import java.util.List;

class TutorContextPayloadBuilder {
    static final int MAX_CODE_LENGTH = 6000;
    static final int MAX_TEXT_LENGTH = 700;

    private TutorContextPayloadBuilder() {
    }

    static ObjectNode safeContext(ObjectMapper objectMapper, TutorHintContext context, String promptVersion) {
        SubmissionDetail submission = context.submission();
        ObjectNode root = objectMapper.createObjectNode();
        root.put("promptVersion", textOrDefault(promptVersion, ""));
        addProblem(root, context.problem());

        ObjectNode attempt = root.putObject("attempt");
        attempt.put("submissionId", textOrDefault(submission.getId(), ""));
        attempt.put("language", textOrDefault(submission.getLanguage(), ""));
        attempt.put("status", submission.getStatus() == null ? "WRONG_ANSWER" : submission.getStatus().toUpperCase());
        attempt.put("passedCount", submission.getPassedCount());
        attempt.put("totalCount", submission.getTotalCount());
        attempt.put("runHiddenTests", submission.isRunHiddenTests());
        attempt.put("compileOutput", compact(submission.getCompileOutput(), MAX_TEXT_LENGTH));
        attempt.put("userCode", compact(submission.getCode(), MAX_CODE_LENGTH));

        ArrayNode visibleFailures = root.putArray("visibleFailures");
        for (TutorHintContext.VisibleFailure failure : context.visibleFailures()) {
            ObjectNode node = visibleFailures.addObject();
            node.put("name", compact(failure.name(), MAX_TEXT_LENGTH));
            node.put("expectedOutput", compact(failure.expectedOutput(), MAX_TEXT_LENGTH));
            node.put("actualOutput", compact(failure.actualOutput(), MAX_TEXT_LENGTH));
            node.put("stderr", compact(failure.stderr(), MAX_TEXT_LENGTH));
            node.put("timedOut", failure.timedOut());
            node.put("exitCode", failure.exitCode());
        }

        ObjectNode hiddenTests = root.putObject("hiddenTests");
        hiddenTests.put("totalCount", context.hiddenTestCount());
        hiddenTests.put("failedCount", context.hiddenFailureCount());
        hiddenTests.put("timedOutCount", context.hiddenTimedOutCount());
        hiddenTests.put(
            "detailPolicy",
            "Hidden test names, inputs, expected outputs, actual outputs, and stderr are not available."
        );
        return root;
    }

    private static void addProblem(ObjectNode root, PublicProblem problem) {
        ObjectNode problemNode = root.putObject("problem");
        if (problem == null) {
            problemNode.put("available", false);
            return;
        }

        problemNode.put("available", true);
        problemNode.put("id", textOrDefault(problem.getId(), ""));
        problemNode.put("title", textOrDefault(problem.getTitle(), ""));
        problemNode.put("difficulty", textOrDefault(problem.getDifficulty(), ""));
        problemNode.put("description", compact(problem.getDescription(), MAX_TEXT_LENGTH));
        problemNode.put("inputFormat", compact(problem.getInputFormat(), MAX_TEXT_LENGTH));
        problemNode.put("outputFormat", compact(problem.getOutputFormat(), MAX_TEXT_LENGTH));

        ArrayNode tags = problemNode.putArray("tags");
        for (String tag : listOrEmpty(problem.getTags())) {
            tags.add(compact(tag, MAX_TEXT_LENGTH));
        }

        ArrayNode constraints = problemNode.putArray("constraints");
        for (String constraint : listOrEmpty(problem.getConstraints())) {
            constraints.add(compact(constraint, MAX_TEXT_LENGTH));
        }

        ArrayNode examples = problemNode.putArray("examples");
        for (Example example : listOrEmpty(problem.getExamples())) {
            ObjectNode node = examples.addObject();
            node.put("input", compact(example.getInput(), MAX_TEXT_LENGTH));
            node.put("output", compact(example.getOutput(), MAX_TEXT_LENGTH));
            node.put("explanation", compact(example.getExplanation(), MAX_TEXT_LENGTH));
        }
    }

    static String compact(String value, int limit) {
        if (value == null) {
            return "";
        }

        String compacted = value.replace("\r\n", "\n").trim();
        if (compacted.length() <= limit) {
            return compacted;
        }
        return compacted.substring(0, limit) + "...";
    }

    private static String textOrDefault(String value, String fallback) {
        if (value == null || value.isBlank()) {
            return fallback == null ? "" : fallback;
        }
        return value;
    }

    private static <T> List<T> listOrEmpty(List<T> values) {
        return values == null ? List.of() : values;
    }
}
