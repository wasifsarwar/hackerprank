package com.hackerprank.problems;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

import jakarta.annotation.PostConstruct;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowCallbackHandler;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

@Repository
public class ProblemRepository {
    private static final String PUBLISHED = "PUBLISHED";
    private static final String DRAFT = "DRAFT";

    private final JdbcTemplate jdbcTemplate;

    public ProblemRepository(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    @PostConstruct
    public void seedProblems() {
        saveSeed(addPairProblem(), 1);
        saveSeed(frequencyProblem(), 2);
    }

    public List<ProblemSummary> findAllSummaries() {
        List<ProblemSummaryRow> rows = jdbcTemplate.query(
            """
                SELECT id, title, difficulty
                FROM problems
                WHERE publication_status = ?
                ORDER BY sort_order, id
                """,
            (rs, rowNum) -> new ProblemSummaryRow(
                rs.getString("id"),
                rs.getString("title"),
                rs.getString("difficulty")
            ),
            PUBLISHED
        );

        Map<String, List<String>> tagsByProblemId = findTagsForProblemIds(
            rows.stream().map(ProblemSummaryRow::id).collect(Collectors.toList())
        );

        return rows.stream()
            .map(row -> new ProblemSummary(
                row.id(),
                row.title(),
                row.difficulty(),
                tagsByProblemId.getOrDefault(row.id(), List.of())
            ))
            .collect(Collectors.toList());
    }

    public List<Problem> findAll() {
        List<String> ids = jdbcTemplate.query(
            """
                SELECT id
                FROM problems
                WHERE publication_status = ?
                ORDER BY sort_order, id
                """,
            (rs, rowNum) -> rs.getString("id"),
            PUBLISHED
        );

        return ids.stream()
            .map(this::findById)
            .flatMap(Optional::stream)
            .collect(Collectors.toList());
    }

    public Optional<Problem> findById(String id) {
        return findProblem(
            """
                SELECT id, title, difficulty, description, input_format, output_format
                FROM problems
                WHERE id = ? AND publication_status = ?
                """,
            id,
            PUBLISHED
        );
    }

    Optional<Problem> findAnyById(String id) {
        return findProblem(
            """
                SELECT id, title, difficulty, description, input_format, output_format
                FROM problems
                WHERE id = ?
                """,
            id
        );
    }

    public boolean existsAnyById(String id) {
        Integer count = jdbcTemplate.queryForObject(
            "SELECT COUNT(*) FROM problems WHERE id = ?",
            Integer.class,
            id
        );
        return count != null && count > 0;
    }

    @Transactional
    public Problem save(Problem problem) {
        return saveWithStatus(problem, PUBLISHED, "generated", null);
    }

    @Transactional
    Problem saveDraftProblem(Problem problem) {
        return saveWithStatus(problem, DRAFT, "generated", null);
    }

    @Transactional
    void publishById(String id) {
        jdbcTemplate.update(
            """
                UPDATE problems
                SET publication_status = ?, updated_at = CURRENT_TIMESTAMP
                WHERE id = ?
                """,
            PUBLISHED,
            id
        );
    }

    @Transactional
    public void deleteById(String id) {
        jdbcTemplate.update("DELETE FROM problems WHERE id = ?", id);
    }

    @Transactional
    void savePrivateArtifacts(
        String problemId,
        Map<String, String> referenceSolutions,
        String generatorTopic,
        String validationStatus,
        GenerationMetadata generationMetadata
    ) {
        GenerationMetadata metadata = generationMetadata == null ? GenerationMetadata.empty() : generationMetadata;
        String pythonReferenceSolution = referenceSolutions == null ? "" : referenceSolutions.getOrDefault("python", "");
        int updated = jdbcTemplate.update(
            """
                UPDATE problem_private_artifacts
                SET reference_solution = ?,
                    generator_topic = ?,
                    validation_status = ?,
                    generator_provider = ?,
                    generator_model_id = ?,
                    generator_prompt_version = ?,
                    generator_prompt_text = ?,
                    generator_parameters_json = ?,
                    validation_errors = ?,
                    validation_summary = ?,
                    intended_technique = ?,
                    updated_at = CURRENT_TIMESTAMP
                WHERE problem_id = ?
                """,
            pythonReferenceSolution,
            generatorTopic,
            validationStatus,
            metadata.provider(),
            metadata.modelId(),
            metadata.promptVersion(),
            metadata.promptText(),
            metadata.parametersJson(),
            metadata.validationErrors(),
            metadata.validationSummary(),
            metadata.intendedTechnique(),
            problemId
        );

        if (updated == 0) {
            jdbcTemplate.update(
                """
                    INSERT INTO problem_private_artifacts (
                        problem_id,
                        reference_solution,
                        generator_topic,
                        validation_status,
                        generator_provider,
                        generator_model_id,
                        generator_prompt_version,
                        generator_prompt_text,
                        generator_parameters_json,
                        validation_errors,
                        validation_summary,
                        intended_technique
                    )
                    VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
                    """,
                problemId,
                pythonReferenceSolution,
                generatorTopic,
                validationStatus,
                metadata.provider(),
                metadata.modelId(),
                metadata.promptVersion(),
                metadata.promptText(),
                metadata.parametersJson(),
                metadata.validationErrors(),
                metadata.validationSummary(),
                metadata.intendedTechnique()
            );
        }

        replaceReferenceSolutions(problemId, referenceSolutions);
    }

    Optional<String> findReferenceSolutionByProblemId(String problemId) {
        return findReferenceSolutionsByProblemId(problemId).entrySet().stream()
            .filter(entry -> entry.getKey().equals("python"))
            .map(Map.Entry::getValue)
            .findFirst()
            .or(() -> jdbcTemplate.query(
                """
                    SELECT reference_solution
                    FROM problem_private_artifacts
                    WHERE problem_id = ?
                    """,
                (rs, rowNum) -> rs.getString("reference_solution"),
                problemId
            ).stream().findFirst());
    }

    Map<String, String> findReferenceSolutionsByProblemId(String problemId) {
        Map<String, String> solutions = new LinkedHashMap<>();
        jdbcTemplate.query(
            """
                SELECT language, code
                FROM problem_reference_solutions
                WHERE problem_id = ?
                ORDER BY language
                """,
            (RowCallbackHandler) rs -> solutions.put(rs.getString("language"), rs.getString("code")),
            problemId
        );
        return solutions;
    }

    Optional<GenerationMetadata> findGenerationMetadataByProblemId(String problemId) {
        return jdbcTemplate.query(
            """
                SELECT generator_provider,
                       generator_model_id,
                       generator_prompt_version,
                       generator_prompt_text,
                       generator_parameters_json,
                       validation_status,
                       validation_errors,
                       validation_summary,
                       intended_technique
                FROM problem_private_artifacts
                WHERE problem_id = ?
                """,
            (rs, rowNum) -> new GenerationMetadata(
                rs.getString("generator_provider"),
                rs.getString("generator_model_id"),
                rs.getString("generator_prompt_version"),
                rs.getString("generator_prompt_text"),
                rs.getString("generator_parameters_json"),
                rs.getString("validation_status"),
                rs.getString("validation_errors"),
                rs.getString("validation_summary"),
                rs.getString("intended_technique")
            ),
            problemId
        ).stream().findFirst();
    }

    private void replaceReferenceSolutions(String problemId, Map<String, String> referenceSolutions) {
        jdbcTemplate.update("DELETE FROM problem_reference_solutions WHERE problem_id = ?", problemId);
        if (referenceSolutions == null || referenceSolutions.isEmpty()) {
            return;
        }

        List<Object[]> batch = new ArrayList<>();
        for (Map.Entry<String, String> entry : referenceSolutions.entrySet()) {
            batch.add(new Object[] { problemId, entry.getKey(), entry.getValue() });
        }
        jdbcTemplate.batchUpdate(
            "INSERT INTO problem_reference_solutions (problem_id, language, code) VALUES (?, ?, ?)",
            batch
        );
    }

    private Problem saveSeed(Problem problem, int sortOrder) {
        return saveWithStatus(problem, PUBLISHED, "seed", sortOrder);
    }

    private Problem saveWithStatus(Problem problem, String publicationStatus, String sourceType, Integer sortOrder) {
        validateProblem(problem);

        int resolvedSortOrder = sortOrder == null
            ? findSortOrder(problem.getId()).orElseGet(this::nextSortOrder)
            : sortOrder;

        int updated = jdbcTemplate.update(
            """
                UPDATE problems
                SET title = ?,
                    difficulty = ?,
                    description = ?,
                    input_format = ?,
                    output_format = ?,
                    publication_status = ?,
                    source_type = ?,
                    sort_order = ?,
                    updated_at = CURRENT_TIMESTAMP
                WHERE id = ?
                """,
            problem.getTitle(),
            problem.getDifficulty(),
            problem.getDescription(),
            problem.getInputFormat(),
            problem.getOutputFormat(),
            publicationStatus,
            sourceType,
            resolvedSortOrder,
            problem.getId()
        );

        if (updated == 0) {
            jdbcTemplate.update(
                """
                    INSERT INTO problems (
                        id,
                        title,
                        difficulty,
                        description,
                        input_format,
                        output_format,
                        publication_status,
                        source_type,
                        sort_order
                    )
                    VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?)
                    """,
                problem.getId(),
                problem.getTitle(),
                problem.getDifficulty(),
                problem.getDescription(),
                problem.getInputFormat(),
                problem.getOutputFormat(),
                publicationStatus,
                sourceType,
                resolvedSortOrder
            );
        }

        replaceChildren(problem);
        return problem;
    }

    private Optional<Problem> findProblem(String sql, Object... args) {
        return jdbcTemplate.query(
            sql,
            (rs, rowNum) -> new ProblemRow(
                rs.getString("id"),
                rs.getString("title"),
                rs.getString("difficulty"),
                rs.getString("description"),
                rs.getString("input_format"),
                rs.getString("output_format")
            ),
            args
        ).stream()
            .findFirst()
            .map(this::hydrateProblem);
    }

    private Problem hydrateProblem(ProblemRow row) {
        return new Problem(
            row.id(),
            row.title(),
            row.difficulty(),
            findTags(row.id()),
            row.description(),
            row.inputFormat(),
            row.outputFormat(),
            findConstraints(row.id()),
            findExamples(row.id()),
            findTestCases(row.id()),
            findStarterCode(row.id())
        );
    }

    private List<String> findTags(String problemId) {
        return jdbcTemplate.query(
            """
                SELECT tag
                FROM problem_tags
                WHERE problem_id = ?
                ORDER BY display_order
                """,
            (rs, rowNum) -> rs.getString("tag"),
            problemId
        );
    }

    private Map<String, List<String>> findTagsForProblemIds(List<String> problemIds) {
        if (problemIds.isEmpty()) {
            return Map.of();
        }

        String placeholders = String.join(",", Collections.nCopies(problemIds.size(), "?"));
        Map<String, List<String>> tagsByProblemId = new LinkedHashMap<>();
        jdbcTemplate.query(
            """
                SELECT problem_id, tag
                FROM problem_tags
                WHERE problem_id IN (
                """ + placeholders + """
                )
                ORDER BY problem_id, display_order
                """,
            (RowCallbackHandler) rs -> tagsByProblemId
                .computeIfAbsent(rs.getString("problem_id"), ignored -> new ArrayList<>())
                .add(rs.getString("tag")),
            problemIds.toArray()
        );
        return tagsByProblemId;
    }

    private List<String> findConstraints(String problemId) {
        return jdbcTemplate.query(
            """
                SELECT constraint_text
                FROM problem_constraints
                WHERE problem_id = ?
                ORDER BY display_order
                """,
            (rs, rowNum) -> rs.getString("constraint_text"),
            problemId
        );
    }

    private List<Example> findExamples(String problemId) {
        return jdbcTemplate.query(
            """
                SELECT input_text, output_text, explanation
                FROM problem_examples
                WHERE problem_id = ?
                ORDER BY display_order
                """,
            (rs, rowNum) -> new Example(
                rs.getString("input_text"),
                rs.getString("output_text"),
                rs.getString("explanation")
            ),
            problemId
        );
    }

    private List<TestCase> findTestCases(String problemId) {
        return jdbcTemplate.query(
            """
                SELECT name, input_text, expected_output, hidden
                FROM problem_test_cases
                WHERE problem_id = ?
                ORDER BY display_order
                """,
            (rs, rowNum) -> new TestCase(
                rs.getString("name"),
                rs.getString("input_text"),
                rs.getString("expected_output"),
                rs.getBoolean("hidden")
            ),
            problemId
        );
    }

    private Map<String, String> findStarterCode(String problemId) {
        Map<String, String> starterCode = new LinkedHashMap<>();
        jdbcTemplate.query(
            """
                SELECT language, code
                FROM problem_starter_code
                WHERE problem_id = ?
                ORDER BY language
                """,
            (RowCallbackHandler) rs -> starterCode.put(rs.getString("language"), rs.getString("code")),
            problemId
        );
        return starterCode;
    }

    private void replaceChildren(Problem problem) {
        deleteChildren(problem.getId());
        insertTags(problem);
        insertConstraints(problem);
        insertExamples(problem);
        insertTestCases(problem);
        insertStarterCode(problem);
    }

    private void deleteChildren(String problemId) {
        jdbcTemplate.update("DELETE FROM problem_tags WHERE problem_id = ?", problemId);
        jdbcTemplate.update("DELETE FROM problem_constraints WHERE problem_id = ?", problemId);
        jdbcTemplate.update("DELETE FROM problem_examples WHERE problem_id = ?", problemId);
        jdbcTemplate.update("DELETE FROM problem_test_cases WHERE problem_id = ?", problemId);
        jdbcTemplate.update("DELETE FROM problem_starter_code WHERE problem_id = ?", problemId);
    }

    private void insertTags(Problem problem) {
        List<Object[]> batch = new ArrayList<>();
        for (int index = 0; index < problem.getTags().size(); index++) {
            batch.add(new Object[] { problem.getId(), index, problem.getTags().get(index) });
        }
        jdbcTemplate.batchUpdate(
            "INSERT INTO problem_tags (problem_id, display_order, tag) VALUES (?, ?, ?)",
            batch
        );
    }

    private void insertConstraints(Problem problem) {
        List<Object[]> batch = new ArrayList<>();
        for (int index = 0; index < problem.getConstraints().size(); index++) {
            batch.add(new Object[] { problem.getId(), index, problem.getConstraints().get(index) });
        }
        jdbcTemplate.batchUpdate(
            "INSERT INTO problem_constraints (problem_id, display_order, constraint_text) VALUES (?, ?, ?)",
            batch
        );
    }

    private void insertExamples(Problem problem) {
        List<Object[]> batch = new ArrayList<>();
        for (int index = 0; index < problem.getExamples().size(); index++) {
            Example example = problem.getExamples().get(index);
            batch.add(new Object[] {
                problem.getId(),
                index,
                example.getInput(),
                example.getOutput(),
                example.getExplanation()
            });
        }
        jdbcTemplate.batchUpdate(
            """
                INSERT INTO problem_examples (problem_id, display_order, input_text, output_text, explanation)
                VALUES (?, ?, ?, ?, ?)
                """,
            batch
        );
    }

    private void insertTestCases(Problem problem) {
        List<Object[]> batch = new ArrayList<>();
        for (int index = 0; index < problem.getTestCases().size(); index++) {
            TestCase testCase = problem.getTestCases().get(index);
            batch.add(new Object[] {
                problem.getId(),
                index,
                testCase.getName(),
                testCase.getInput(),
                testCase.getExpectedOutput(),
                testCase.isHidden()
            });
        }
        jdbcTemplate.batchUpdate(
            """
                INSERT INTO problem_test_cases (
                    problem_id,
                    display_order,
                    name,
                    input_text,
                    expected_output,
                    hidden
                )
                VALUES (?, ?, ?, ?, ?, ?)
                """,
            batch
        );
    }

    private void insertStarterCode(Problem problem) {
        List<Object[]> batch = new ArrayList<>();
        for (Map.Entry<String, String> entry : problem.getStarterCode().entrySet()) {
            batch.add(new Object[] { problem.getId(), entry.getKey(), entry.getValue() });
        }
        jdbcTemplate.batchUpdate(
            "INSERT INTO problem_starter_code (problem_id, language, code) VALUES (?, ?, ?)",
            batch
        );
    }

    private Optional<Integer> findSortOrder(String problemId) {
        return jdbcTemplate.query(
            "SELECT sort_order FROM problems WHERE id = ?",
            (rs, rowNum) -> rs.getInt("sort_order"),
            problemId
        ).stream().findFirst();
    }

    private int nextSortOrder() {
        Integer next = jdbcTemplate.queryForObject(
            "SELECT COALESCE(MAX(sort_order), 0) + 1 FROM problems",
            Integer.class
        );
        return next == null ? 1 : next;
    }

    private void validateProblem(Problem problem) {
        if (problem == null || problem.getId() == null || problem.getId().isBlank()) {
            throw new IllegalArgumentException("Problem id is required");
        }
    }

    private Problem addPairProblem() {
        Map<String, String> starterCode = new LinkedHashMap<>();
        starterCode.put("python",
            "import sys\n\n" +
            "def main():\n" +
            "    numbers = list(map(int, sys.stdin.read().strip().split()))\n" +
            "    # TODO: print the sum of the two numbers\n" +
            "    print(0)\n\n" +
            "if __name__ == \"__main__\":\n" +
            "    main()\n");
        starterCode.put("java",
            "import java.util.*;\n\n" +
            "public class Main {\n" +
            "    public static void main(String[] args) {\n" +
            "        Scanner scanner = new Scanner(System.in);\n" +
            "        int a = scanner.nextInt();\n" +
            "        int b = scanner.nextInt();\n" +
            "        // TODO: print the sum of a and b\n" +
            "        System.out.println(0);\n" +
            "    }\n" +
            "}\n");

        return new Problem(
            "add-a-pair",
            "Add a Pair",
            "Easy",
            Arrays.asList("Warmup", "Input/Output"),
            "Given two integers, print their sum. This tiny warmup makes sure your code can read stdin and write stdout.",
            "A single line containing two space-separated integers: a and b.",
            "Print a single integer: the sum of a and b.",
            Arrays.asList("-10,000 <= a, b <= 10,000"),
            Arrays.asList(
                new Example("3 5\n", "8\n", "3 plus 5 equals 8."),
                new Example("-2 9\n", "7\n", "-2 plus 9 equals 7.")
            ),
            Arrays.asList(
                new TestCase("Sample 1", "3 5\n", "8\n", false),
                new TestCase("Sample 2", "-2 9\n", "7\n", false),
                new TestCase("Hidden 1", "10000 -9999\n", "1\n", true),
                new TestCase("Hidden 2", "0 0\n", "0\n", true)
            ),
            starterCode
        );
    }

    private Problem frequencyProblem() {
        Map<String, String> starterCode = new LinkedHashMap<>();
        starterCode.put("python",
            "import sys\n\n" +
            "def main():\n" +
            "    tokens = sys.stdin.read().strip().split()\n" +
            "    n = int(tokens[0])\n" +
            "    words = tokens[1:]\n" +
            "    # TODO: print the word that appears most often\n" +
            "    print(words[0] if words else \"\")\n\n" +
            "if __name__ == \"__main__\":\n" +
            "    main()\n");
        starterCode.put("java",
            "import java.util.*;\n\n" +
            "public class Main {\n" +
            "    public static void main(String[] args) {\n" +
            "        Scanner scanner = new Scanner(System.in);\n" +
            "        int n = scanner.nextInt();\n" +
            "        String answer = \"\";\n" +
            "        for (int i = 0; i < n; i++) {\n" +
            "            String word = scanner.next();\n" +
            "            if (i == 0) {\n" +
            "                answer = word;\n" +
            "            }\n" +
            "        }\n" +
            "        // TODO: print the word with the highest frequency\n" +
            "        System.out.println(answer);\n" +
            "    }\n" +
            "}\n");

        return new Problem(
            "most-frequent-word",
            "Most Frequent Word",
            "Easy",
            Arrays.asList("Maps", "Counting"),
            "Given a list of lowercase words, print the word that appears the most often. If there is a tie, print the word that appears first among the tied words in the original list.",
            "The first line contains n. The second line contains n lowercase words separated by spaces.",
            "Print the most frequent word.",
            Arrays.asList("1 <= n <= 1000", "Each word contains only lowercase English letters."),
            Arrays.asList(
                new Example("5\nred blue red green blue\n", "red\n", "red and blue both appear twice, but red appears first."),
                new Example("4\ncode code test code\n", "code\n", "code appears three times.")
            ),
            Arrays.asList(
                new TestCase("Sample 1", "5\nred blue red green blue\n", "red\n", false),
                new TestCase("Sample 2", "4\ncode code test code\n", "code\n", false),
                new TestCase("Hidden 1", "6\na b c b c c\n", "c\n", true),
                new TestCase("Hidden 2", "3\nz y z\n", "z\n", true)
            ),
            starterCode
        );
    }

    private record ProblemSummaryRow(String id, String title, String difficulty) {
    }

    private record ProblemRow(
        String id,
        String title,
        String difficulty,
        String description,
        String inputFormat,
        String outputFormat
    ) {
    }
}
