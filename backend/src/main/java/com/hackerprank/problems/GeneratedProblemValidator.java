package com.hackerprank.problems;

import com.hackerprank.submissions.SubmissionResult;
import com.hackerprank.submissions.SubmissionService;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.stream.IntStream;

import org.springframework.stereotype.Component;

@Component
class GeneratedProblemValidator {
    private static final List<String> REQUIRED_LANGUAGES = List.of("python", "java");

    private final SubmissionService submissionService;

    GeneratedProblemValidator(SubmissionService submissionService) {
        this.submissionService = submissionService;
    }

    GeneratedProblemValidationReport validate(GeneratedProblemSpec spec) {
        List<String> errors = new ArrayList<>();
        validateShape(spec, errors);
        if (!errors.isEmpty()) {
            throw new IllegalStateException("Generated problem failed schema validation: " + String.join("; ", errors));
        }

        validateExamples(spec, errors);
        validateReferenceSolutions(spec, errors);
        if (!errors.isEmpty()) {
            throw new IllegalStateException("Generated problem failed reference validation: " + String.join("; ", errors));
        }

        return GeneratedProblemValidationReport.validated(
            "Schema checks passed and Python/Java reference solutions passed examples plus all visible and hidden tests."
        );
    }

    private void validateShape(GeneratedProblemSpec spec, List<String> errors) {
        if (spec == null) {
            errors.add("spec is required");
            return;
        }

        Problem problem = spec.problem();
        if (problem == null) {
            errors.add("problem is required");
            return;
        }

        requireText(problem.getId(), "problem.id", errors);
        requireText(problem.getTitle(), "problem.title", errors);
        requireDifficulty(problem.getDifficulty(), errors);
        requireText(problem.getDescription(), "problem.description", errors);
        requireText(problem.getInputFormat(), "problem.inputFormat", errors);
        requireText(problem.getOutputFormat(), "problem.outputFormat", errors);
        requireNonEmpty(problem.getTags(), "problem.tags", errors);
        requireNonEmpty(problem.getConstraints(), "problem.constraints", errors);
        requireNonEmpty(problem.getExamples(), "problem.examples", errors);
        requireNonEmpty(problem.getTestCases(), "problem.testCases", errors);

        if (problem.getTestCases() != null && !problem.getTestCases().isEmpty()) {
            long visibleTests = problem.getTestCases().stream().filter(testCase -> !testCase.isHidden()).count();
            long hiddenTests = problem.getTestCases().stream().filter(TestCase::isHidden).count();
            if (visibleTests == 0) {
                errors.add("problem.testCases needs at least one visible test");
            }
            if (hiddenTests == 0) {
                errors.add("problem.testCases needs at least one hidden test");
            }
        }

        validateLanguageMap(problem.getStarterCode(), "starterCode", errors);
        validateLanguageMap(spec.referenceSolutions(), "referenceSolutions", errors);
    }

    private void validateReferenceSolutions(GeneratedProblemSpec spec, List<String> errors) {
        for (String language : REQUIRED_LANGUAGES) {
            String code = spec.referenceSolutions().get(language);
            SubmissionResult result = submissionService.run(spec.problem(), language, code, true);
            if (!"ACCEPTED".equals(result.getStatus())) {
                errors.add(language + " reference solution returned " + result.getStatus());
            }
        }
    }

    private void validateExamples(GeneratedProblemSpec spec, List<String> errors) {
        Problem problem = spec.problem();
        List<TestCase> exampleCases = IntStream.range(0, problem.getExamples().size())
            .mapToObj(index -> {
                Example example = problem.getExamples().get(index);
                return new TestCase("Example " + (index + 1), example.getInput(), example.getOutput(), false);
            })
            .toList();
        Problem exampleProblem = new Problem(
            problem.getId(),
            problem.getTitle(),
            problem.getDifficulty(),
            problem.getTags(),
            problem.getDescription(),
            problem.getInputFormat(),
            problem.getOutputFormat(),
            problem.getConstraints(),
            problem.getExamples(),
            exampleCases,
            problem.getStarterCode()
        );

        for (String language : REQUIRED_LANGUAGES) {
            String code = spec.referenceSolutions().get(language);
            SubmissionResult result = submissionService.run(exampleProblem, language, code, true);
            if (!"ACCEPTED".equals(result.getStatus())) {
                errors.add(language + " reference solution returned " + result.getStatus() + " for examples");
            }
        }
    }

    private void validateLanguageMap(Map<String, String> values, String label, List<String> errors) {
        if (values == null || values.isEmpty()) {
            errors.add(label + " is required");
            return;
        }

        for (String language : REQUIRED_LANGUAGES) {
            requireText(values.get(language), label + "." + language, errors);
        }
    }

    private void requireDifficulty(String difficulty, List<String> errors) {
        if (difficulty == null) {
            errors.add("problem.difficulty is required");
            return;
        }

        String normalized = difficulty.toLowerCase(Locale.ROOT);
        if (!normalized.equals("easy") && !normalized.equals("medium") && !normalized.equals("hard")) {
            errors.add("problem.difficulty must be Easy, Medium, or Hard");
        }
    }

    private void requireText(String value, String label, List<String> errors) {
        if (value == null || value.isBlank()) {
            errors.add(label + " is required");
        }
    }

    private void requireNonEmpty(List<?> value, String label, List<String> errors) {
        if (value == null || value.isEmpty()) {
            errors.add(label + " is required");
        }
    }
}
