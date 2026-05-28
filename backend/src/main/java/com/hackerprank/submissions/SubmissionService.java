package com.hackerprank.submissions;

import com.hackerprank.problems.Problem;
import com.hackerprank.problems.ProblemRepository;
import com.hackerprank.problems.TestCase;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.stream.Collectors;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

@Service
public class SubmissionService {
    private static final long COMPILE_TIMEOUT_MS = 5_000;
    private static final long RUN_TIMEOUT_MS = 2_000;

    private final ProblemRepository problemRepository;
    private final SandboxRunner sandboxRunner;
    private final Path workspaceRoot;

    public SubmissionService(
        ProblemRepository problemRepository,
        SandboxRunner sandboxRunner,
        @Value("${hackerprank.runner.workspace-root:.hackerprank-submissions}") String workspaceRoot
    ) {
        this.problemRepository = problemRepository;
        this.sandboxRunner = sandboxRunner;
        this.workspaceRoot = Paths.get(workspaceRoot).toAbsolutePath().normalize();
    }

    public SubmissionResult run(SubmissionRequest request) {
        Problem problem = problemRepository.findById(request.getProblemId())
            .orElseThrow(() -> new IllegalArgumentException("Problem not found: " + request.getProblemId()));

        return run(problem, request.getLanguage(), request.getCode(), request.isRunHiddenTests());
    }

    public SubmissionResult run(Problem problem, String requestedLanguage, String code, boolean runHiddenTests) {
        String language = normalizeLanguage(requestedLanguage);
        List<TestCase> cases = problem.getTestCases().stream()
            .filter(testCase -> runHiddenTests || !testCase.isHidden())
            .collect(Collectors.toList());

        Path workspace = null;
        try {
            Files.createDirectories(workspaceRoot);
            workspace = Files.createTempDirectory(workspaceRoot, "submission-");
            String compileOutput = prepareSubmission(language, code, workspace);
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
                if (!result.isPassed() && !result.isTimedOut() && result.getExitCode() != 0) {
                    runtimeError = true;
                }
            }

            String status = statusFor(passedCount, cases.size(), timedOut, runtimeError);
            return new SubmissionResult(status, passedCount, cases.size(), "", results);
        } catch (InterruptedException exception) {
            Thread.currentThread().interrupt();
            return new SubmissionResult("RUNTIME_ERROR", 0, cases.size(), exception.getMessage(), new ArrayList<>());
        } catch (IOException exception) {
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
        String source = code == null ? "" : code;

        if (language.equals("python")) {
            Files.write(workspace.resolve("main.py"), source.getBytes(StandardCharsets.UTF_8));
            return null;
        }

        Files.write(workspace.resolve("Main.java"), source.getBytes(StandardCharsets.UTF_8));
        ProcessResult compile = sandboxRunner.run(
            language,
            List.of("javac", "Main.java"),
            workspace,
            "",
            COMPILE_TIMEOUT_MS
        );
        if (compile.getExitCode() != 0 || compile.isTimedOut()) {
            String reason = compile.isTimedOut() ? "Compilation timed out.\n" : "";
            return reason + compile.getStdout() + compile.getStderr();
        }
        return null;
    }

    private TestCaseResult runCase(String language, Path workspace, TestCase testCase) throws IOException, InterruptedException {
        List<String> command = language.equals("python")
            ? List.of("python3", "main.py")
            : List.of("java", "Main");

        ProcessResult processResult = sandboxRunner.run(language, command, workspace, testCase.getInput(), RUN_TIMEOUT_MS);
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
            processResult.getExitCode(),
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
