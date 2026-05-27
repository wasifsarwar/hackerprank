package com.hackerprank.submissions;

import com.hackerprank.problems.Problem;
import com.hackerprank.problems.ProblemRepository;
import com.hackerprank.problems.TestCase;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;

@Service
public class SubmissionService {
    private static final long COMPILE_TIMEOUT_MS = 5_000;
    private static final long RUN_TIMEOUT_MS = 2_000;

    private final ProblemRepository problemRepository;
    private final ProcessRunner processRunner;

    public SubmissionService(ProblemRepository problemRepository, ProcessRunner processRunner) {
        this.problemRepository = problemRepository;
        this.processRunner = processRunner;
    }

    public SubmissionResult run(SubmissionRequest request) {
        Problem problem = problemRepository.findById(request.getProblemId())
            .orElseThrow(() -> new IllegalArgumentException("Problem not found: " + request.getProblemId()));

        String language = normalizeLanguage(request.getLanguage());
        List<TestCase> cases = problem.getTestCases().stream()
            .filter(testCase -> request.isRunHiddenTests() || !testCase.isHidden())
            .collect(Collectors.toList());

        Path workspace = null;
        try {
            workspace = Files.createTempDirectory("hackerprank-submission-");
            String compileOutput = prepareSubmission(language, request.getCode(), workspace);
            if (compileOutput != null) {
                return new SubmissionResult("COMPILE_ERROR", 0, cases.size(), compileOutput, new ArrayList<>());
            }

            List<TestCaseResult> results = new ArrayList<>();
            for (TestCase testCase : cases) {
                results.add(runCase(language, workspace, testCase));
            }

            int passedCount = 0;
            boolean timedOut = false;
            boolean runtimeError = false;
            for (TestCaseResult result : results) {
                if (result.isPassed()) {
                    passedCount++;
                }
                if (result.isTimedOut()) {
                    timedOut = true;
                }
                if (!result.isPassed() && !result.isTimedOut() && result.getStderr() != null && !result.getStderr().isEmpty()) {
                    runtimeError = true;
                }
            }

            String status = statusFor(passedCount, cases.size(), timedOut, runtimeError);
            return new SubmissionResult(status, passedCount, cases.size(), "", results);
        } catch (IOException | InterruptedException exception) {
            Thread.currentThread().interrupt();
            return new SubmissionResult("RUNTIME_ERROR", 0, cases.size(), exception.getMessage(), new ArrayList<>());
        } finally {
            deleteRecursively(workspace);
        }
    }

    private String normalizeLanguage(String language) {
        if (language == null) {
            throw new IllegalArgumentException("Language is required");
        }

        String normalized = language.toLowerCase(Locale.ROOT);
        if (!normalized.equals("python") && !normalized.equals("java")) {
            throw new IllegalArgumentException("Unsupported language: " + language);
        }
        return normalized;
    }

    private String prepareSubmission(String language, String code, Path workspace) throws IOException, InterruptedException {
        if (language.equals("python")) {
            Files.write(workspace.resolve("main.py"), code.getBytes(StandardCharsets.UTF_8));
            return null;
        }

        Files.write(workspace.resolve("Main.java"), code.getBytes(StandardCharsets.UTF_8));
        ProcessResult compile = processRunner.run(Arrays.asList("javac", "Main.java"), workspace, "", COMPILE_TIMEOUT_MS);
        if (compile.getExitCode() != 0 || compile.isTimedOut()) {
            String reason = compile.isTimedOut() ? "Compilation timed out.\n" : "";
            return reason + compile.getStdout() + compile.getStderr();
        }
        return null;
    }

    private TestCaseResult runCase(String language, Path workspace, TestCase testCase) throws IOException, InterruptedException {
        List<String> command = language.equals("python")
            ? Arrays.asList("python3", "main.py")
            : Arrays.asList("java", "-cp", workspace.toString(), "Main");

        ProcessResult processResult = processRunner.run(command, workspace, testCase.getInput(), RUN_TIMEOUT_MS);
        boolean passed = processResult.getExitCode() == 0
            && !processResult.isTimedOut()
            && normalizeOutput(processResult.getStdout()).equals(normalizeOutput(testCase.getExpectedOutput()));

        return new TestCaseResult(
            testCase.getName(),
            testCase.isHidden(),
            passed,
            testCase.isHidden() ? null : testCase.getExpectedOutput(),
            processResult.getStdout(),
            processResult.getStderr(),
            processResult.isTimedOut(),
            processResult.getRuntimeMs()
        );
    }

    private String statusFor(int passedCount, int totalCount, boolean timedOut, boolean runtimeError) {
        if (timedOut) {
            return "TIME_LIMIT_EXCEEDED";
        }
        if (runtimeError) {
            return "RUNTIME_ERROR";
        }
        if (passedCount == totalCount) {
            return "ACCEPTED";
        }
        return "WRONG_ANSWER";
    }

    private String normalizeOutput(String output) {
        return output.replaceAll("\\s+$", "");
    }

    private void deleteRecursively(Path root) {
        if (root == null) {
            return;
        }

        try {
            Files.walk(root)
                .sorted(Comparator.reverseOrder())
                .forEach(path -> {
                    try {
                        Files.deleteIfExists(path);
                    } catch (IOException ignored) {
                    }
                });
        } catch (IOException ignored) {
        }
    }
}
