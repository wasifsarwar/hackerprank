package com.hackerprank.problems;

import com.hackerprank.submissions.SubmissionResult;
import com.hackerprank.submissions.SubmissionService;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.regex.Pattern;
import java.util.stream.IntStream;

import org.springframework.stereotype.Component;

@Component
class GeneratedProblemValidator {
    private static final List<String> REQUIRED_LANGUAGES = List.of("python", "java");
    private static final Pattern PYTHON_HELPER_DEFINITION = Pattern.compile("(?m)^\\s*def\\s+(?!main\\b)[A-Za-z_]\\w*\\s*\\(");
    private static final Pattern PYTHON_STDIN_READ = Pattern.compile(
        "(?s)(sys\\.stdin\\.read\\s*\\(|sys\\.stdin\\.readline\\s*\\(|input\\s*\\()"
    );
    private static final Pattern JAVA_HELPER_DEFINITION = Pattern.compile(
        "(?s)(?:public|private|protected)?\\s*static\\s+(?!void\\s+main\\b)[A-Za-z_<>, ?\\[\\]]+\\s+[A-Za-z_]\\w*\\s*\\([^)]*\\)\\s*\\{"
    );
    private static final Pattern JAVA_STDIN_READ = Pattern.compile(
        "(?s)(new\\s+Scanner\\s*\\(\\s*System\\.in\\s*\\)|new\\s+BufferedReader\\s*\\([^;]*System\\.in|System\\.in\\.read\\s*\\()"
    );
    private static final Pattern JAVA_METHOD_DEFINITION = Pattern.compile(
        "(?s)(?:public|private|protected)?\\s*static\\s+([A-Za-z_<>, ?\\[\\]]+)\\s+([A-Za-z_]\\w*)\\s*\\(([^)]*)\\)\\s*\\{"
    );
    private static final Pattern PYTHON_FUNCTION_DEFINITION = Pattern.compile(
        "(?m)^\\s*def\\s+([A-Za-z_]\\w*)\\s*\\(([^)]*)\\)\\s*(?:->\\s*[^:]+)?\\s*:"
    );

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
        validateStarterCodeContract(spec, errors);
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
        requireText(problem.getScenario(), "problem.scenario", errors);
        requireMinimumLength(problem.getScenario(), "problem.scenario", 80, errors);
        requireText(problem.getTask(), "problem.task", errors);
        requireMinimumLength(problem.getTask(), "problem.task", 60, errors);
        requireText(problem.getJavaSignature(), "problem.javaSignature", errors);
        requireText(problem.getPythonSignature(), "problem.pythonSignature", errors);
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
        validateSignatureContract(problem, errors);
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
            problem.getScenario(),
            problem.getTask(),
            problem.getJavaSignature(),
            problem.getPythonSignature(),
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

    private void validateStarterCodeContract(GeneratedProblemSpec spec, List<String> errors) {
        Map<String, String> starterCode = spec.problem().getStarterCode();
        validatePythonStarterCode(starterCode.get("python"), errors);
        validateJavaStarterCode(starterCode.get("java"), errors);
    }

    private void validateSignatureContract(Problem problem, List<String> errors) {
        Map<String, String> starterCode = problem.getStarterCode();
        if (starterCode == null) {
            return;
        }

        String javaCode = starterCode.getOrDefault("java", "");
        JavaHelperSignature javaSignature = parseJavaSignature(problem.getJavaSignature());
        if (javaSignature != null) {
            if (!hasMatchingJavaDefinition(javaCode, javaSignature)) {
                errors.add("starterCode.java must define the declared javaSignature helper method");
            } else if (!hasInvocationWithArity(javaCode, javaSignature.methodName(), javaSignature.parameterTypes().size())) {
                errors.add("starterCode.java must call the declared javaSignature helper method");
            }
        }

        String pythonCode = starterCode.getOrDefault("python", "");
        PythonHelperSignature pythonSignature = parsePythonSignature(problem.getPythonSignature());
        if (pythonSignature != null) {
            if (!hasMatchingPythonDefinition(pythonCode, pythonSignature)) {
                errors.add("starterCode.python must define the declared pythonSignature helper function");
            } else if (!hasInvocationWithArity(pythonCode, pythonSignature.functionName(), pythonSignature.parameters().size())) {
                errors.add("starterCode.python must call the declared pythonSignature helper function");
            }
        }
    }

    private JavaHelperSignature parseJavaSignature(String signature) {
        if (signature == null || signature.isBlank()) {
            return null;
        }

        int openParen = signature.indexOf('(');
        int closeParen = signature.lastIndexOf(')');
        if (openParen <= 0 || closeParen <= openParen) {
            return null;
        }

        String beforeParen = signature.substring(0, openParen).trim();
        String parameters = signature.substring(openParen + 1, closeParen).trim();
        String[] tokens = beforeParen.split("\\s+");
        if (tokens.length < 2) {
            return null;
        }

        String methodName = tokens[tokens.length - 1];
        int returnTypeIndex = tokens.length - 2;
        while (returnTypeIndex >= 0 && isJavaModifier(tokens[returnTypeIndex])) {
            returnTypeIndex--;
        }
        if (returnTypeIndex < 0 || !methodName.matches("[A-Za-z_]\\w*")) {
            return null;
        }

        return new JavaHelperSignature(
            methodName,
            normalizeJavaType(tokens[returnTypeIndex]),
            parseJavaParameterTypes(parameters)
        );
    }

    private boolean hasMatchingJavaDefinition(String code, JavaHelperSignature signature) {
        if (code == null || code.isBlank()) {
            return false;
        }

        var matcher = JAVA_METHOD_DEFINITION.matcher(code);
        while (matcher.find()) {
            if (!matcher.group(2).equals(signature.methodName())) {
                continue;
            }

            List<String> parameterTypes = parseJavaParameterTypes(matcher.group(3));
            if (
                normalizeJavaType(matcher.group(1)).equals(signature.returnType()) &&
                parameterTypes.equals(signature.parameterTypes())
            ) {
                return true;
            }
        }

        return false;
    }

    private PythonHelperSignature parsePythonSignature(String signature) {
        if (signature == null || signature.isBlank()) {
            return null;
        }

        var matcher = Pattern.compile("\\bdef\\s+([A-Za-z_]\\w*)\\s*\\(([^)]*)\\)").matcher(signature);
        if (!matcher.find()) {
            return null;
        }

        return new PythonHelperSignature(matcher.group(1), parsePythonParameters(matcher.group(2)));
    }

    private boolean hasMatchingPythonDefinition(String code, PythonHelperSignature signature) {
        if (code == null || code.isBlank()) {
            return false;
        }

        var matcher = PYTHON_FUNCTION_DEFINITION.matcher(code);
        while (matcher.find()) {
            if (matcher.group(1).equals(signature.functionName()) && parsePythonParameters(matcher.group(2)).equals(signature.parameters())) {
                return true;
            }
        }

        return false;
    }

    private boolean hasInvocationWithArity(String code, String functionName, int arity) {
        if (code == null || code.isBlank() || functionName == null || functionName.isBlank()) {
            return false;
        }

        var matcher = Pattern.compile("\\b" + Pattern.quote(functionName) + "\\s*\\(([^)]*)\\)").matcher(code);
        while (matcher.find()) {
            if (isDefinitionInvocation(code, matcher.start())) {
                continue;
            }
            if (splitTopLevel(matcher.group(1)).size() == arity) {
                return true;
            }
        }

        return false;
    }

    private boolean isDefinitionInvocation(String code, int invocationStart) {
        int lineStart = code.lastIndexOf('\n', Math.max(0, invocationStart - 1)) + 1;
        String prefix = code.substring(lineStart, invocationStart);
        return prefix.matches("\\s*def\\s+[A-Za-z_]\\w*\\s*")
            || prefix.matches("(?s).*\\bstatic\\s+[A-Za-z_<>, ?\\[\\]]+\\s*");
    }

    private List<String> parseJavaParameterTypes(String parameters) {
        return splitTopLevel(parameters).stream()
            .map(String::trim)
            .filter(parameter -> !parameter.isBlank())
            .map(this::javaParameterType)
            .map(this::normalizeJavaType)
            .toList();
    }

    private String javaParameterType(String parameter) {
        String cleaned = parameter.replaceAll("@\\w+(?:\\([^)]*\\))?\\s*", "").trim();
        int lastSpace = cleaned.lastIndexOf(' ');
        return lastSpace > 0 ? cleaned.substring(0, lastSpace) : cleaned;
    }

    private String normalizeJavaType(String type) {
        return type == null ? "" : type.replaceAll("\\s+", "").replace("...", "[]");
    }

    private boolean isJavaModifier(String token) {
        return token.equals("public")
            || token.equals("private")
            || token.equals("protected")
            || token.equals("static")
            || token.equals("final");
    }

    private List<String> parsePythonParameters(String parameters) {
        return splitTopLevel(parameters).stream()
            .map(String::trim)
            .filter(parameter -> !parameter.isBlank())
            .map(parameter -> parameter.split("=", 2)[0].trim())
            .map(parameter -> parameter.split(":", 2)[0].trim())
            .toList();
    }

    private List<String> splitTopLevel(String value) {
        if (value == null || value.isBlank()) {
            return List.of();
        }

        List<String> parts = new ArrayList<>();
        int depth = 0;
        int start = 0;
        for (int index = 0; index < value.length(); index++) {
            char current = value.charAt(index);
            if (current == '<' || current == '[' || current == '(') {
                depth++;
            } else if (current == '>' || current == ']' || current == ')') {
                depth = Math.max(0, depth - 1);
            } else if (current == ',' && depth == 0) {
                parts.add(value.substring(start, index));
                start = index + 1;
            }
        }
        parts.add(value.substring(start));
        return parts;
    }

    private void validatePythonStarterCode(String code, List<String> errors) {
        if (code == null || code.isBlank()) {
            return;
        }

        if (!code.contains("def main(")) {
            errors.add("starterCode.python must include a main function that handles stdin/stdout");
        }
        if (!code.contains("__main__")) {
            errors.add("starterCode.python must call main from the __main__ guard");
        }
        if (!PYTHON_STDIN_READ.matcher(code).find()) {
            errors.add("starterCode.python main must read stdin before calling the TODO helper function");
        }
        if (!PYTHON_HELPER_DEFINITION.matcher(code).find()) {
            errors.add("starterCode.python must include a named TODO helper function");
        }
    }

    private void validateJavaStarterCode(String code, List<String> errors) {
        if (code == null || code.isBlank()) {
            return;
        }

        if (!code.contains("public static void main(String[] args)")) {
            errors.add("starterCode.java must include public static void main(String[] args)");
        }
        if (!JAVA_STDIN_READ.matcher(code).find()) {
            errors.add("starterCode.java main must read System.in before calling the TODO helper method");
        }
        if (!JAVA_HELPER_DEFINITION.matcher(code).find()) {
            errors.add("starterCode.java must include a named static TODO helper method");
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

    private void requireMinimumLength(String value, String label, int minimumLength, List<String> errors) {
        if (value != null && !value.isBlank() && value.trim().length() < minimumLength) {
            errors.add(label + " must be at least " + minimumLength + " characters");
        }
    }

    private void requireNonEmpty(List<?> value, String label, List<String> errors) {
        if (value == null || value.isEmpty()) {
            errors.add(label + " is required");
        }
    }

    private record JavaHelperSignature(String methodName, String returnType, List<String> parameterTypes) {}

    private record PythonHelperSignature(String functionName, List<String> parameters) {}
}
