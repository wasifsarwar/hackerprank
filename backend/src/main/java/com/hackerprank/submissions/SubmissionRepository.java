package com.hackerprank.submissions;

import com.hackerprank.problems.Problem;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.time.Instant;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

@Repository
public class SubmissionRepository {
    private static final int DEFAULT_LIMIT = 25;
    private static final int MAX_LIMIT = 100;

    private final JdbcTemplate jdbcTemplate;

    public SubmissionRepository(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    @Transactional
    public SubmissionResult save(
        Problem problem,
        String language,
        String code,
        boolean runHiddenTests,
        SubmissionResult result
    ) {
        String submissionId = UUID.randomUUID().toString();
        Instant createdAt = Instant.now();

        jdbcTemplate.update(
            """
                INSERT INTO submissions (
                    id,
                    problem_id,
                    problem_title,
                    problem_difficulty,
                    language,
                    code,
                    run_hidden_tests,
                    status,
                    passed_count,
                    total_count,
                    compile_output,
                    created_at
                )
                VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
                """,
            submissionId,
            problem.getId(),
            problem.getTitle(),
            problem.getDifficulty(),
            language,
            code == null ? "" : code,
            runHiddenTests,
            result.getStatus(),
            result.getPassedCount(),
            result.getTotalCount(),
            result.getCompileOutput() == null ? "" : result.getCompileOutput(),
            Timestamp.from(createdAt)
        );

        insertTestResults(submissionId, result.getResults());
        return result.withPersistence(submissionId, createdAt);
    }

    public List<SubmissionSummary> findRecent(String problemId, Integer limit) {
        int normalizedLimit = normalizeLimit(limit);
        if (problemId != null && !problemId.isBlank()) {
            return jdbcTemplate.query(
                """
                    SELECT id,
                           problem_id,
                           problem_title,
                           problem_difficulty,
                           language,
                           status,
                           passed_count,
                           total_count,
                           created_at
                    FROM submissions
                    WHERE problem_id = ?
                    ORDER BY created_at DESC
                    LIMIT ?
                    """,
                this::summaryFromRow,
                problemId,
                normalizedLimit
            );
        }

        return jdbcTemplate.query(
            """
                SELECT id,
                       problem_id,
                       problem_title,
                       problem_difficulty,
                       language,
                       status,
                       passed_count,
                       total_count,
                       created_at
                FROM submissions
                ORDER BY created_at DESC
                LIMIT ?
                """,
            this::summaryFromRow,
            normalizedLimit
        );
    }

    public Optional<SubmissionDetail> findById(String id) {
        return jdbcTemplate.query(
            """
                SELECT id,
                       problem_id,
                       problem_title,
                       problem_difficulty,
                       language,
                       code,
                       run_hidden_tests,
                       status,
                       passed_count,
                       total_count,
                       compile_output,
                       created_at
                FROM submissions
                WHERE id = ?
                """,
            (rs, rowNum) -> detailFromRow(rs),
            id
        ).stream().findFirst();
    }

    private void insertTestResults(String submissionId, List<TestCaseResult> results) {
        if (results.isEmpty()) {
            return;
        }

        List<Object[]> batch = new ArrayList<>();
        for (int index = 0; index < results.size(); index++) {
            TestCaseResult result = results.get(index);
            batch.add(new Object[] {
                submissionId,
                index,
                result.getName(),
                result.isHidden(),
                result.isPassed(),
                result.getExpectedOutput(),
                result.getActualOutput(),
                result.getStderr(),
                result.isTimedOut(),
                result.getExitCode(),
                result.getRuntimeMs()
            });
        }

        jdbcTemplate.batchUpdate(
            """
                INSERT INTO submission_test_results (
                    submission_id,
                    display_order,
                    test_name,
                    hidden,
                    passed,
                    expected_output,
                    actual_output,
                    stderr,
                    timed_out,
                    exit_code,
                    runtime_ms
                )
                VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
                """,
            batch
        );
    }

    private SubmissionSummary summaryFromRow(ResultSet rs, int rowNum) throws SQLException {
        return new SubmissionSummary(
            rs.getString("id"),
            rs.getString("problem_id"),
            rs.getString("problem_title"),
            rs.getString("problem_difficulty"),
            rs.getString("language"),
            rs.getString("status"),
            rs.getInt("passed_count"),
            rs.getInt("total_count"),
            instant(rs, "created_at")
        );
    }

    private SubmissionDetail detailFromRow(ResultSet rs) throws SQLException {
        String id = rs.getString("id");
        return new SubmissionDetail(
            id,
            rs.getString("problem_id"),
            rs.getString("problem_title"),
            rs.getString("problem_difficulty"),
            rs.getString("language"),
            rs.getString("code"),
            rs.getBoolean("run_hidden_tests"),
            rs.getString("status"),
            rs.getInt("passed_count"),
            rs.getInt("total_count"),
            rs.getString("compile_output"),
            instant(rs, "created_at"),
            findTestResults(id)
        );
    }

    private List<TestCaseResult> findTestResults(String submissionId) {
        return jdbcTemplate.query(
            """
                SELECT test_name,
                       hidden,
                       passed,
                       expected_output,
                       actual_output,
                       stderr,
                       timed_out,
                       exit_code,
                       runtime_ms
                FROM submission_test_results
                WHERE submission_id = ?
                ORDER BY display_order
                """,
            (rs, rowNum) -> new TestCaseResult(
                rs.getString("test_name"),
                rs.getBoolean("hidden"),
                rs.getBoolean("passed"),
                rs.getString("expected_output"),
                rs.getString("actual_output"),
                rs.getString("stderr"),
                rs.getBoolean("timed_out"),
                rs.getInt("exit_code"),
                rs.getLong("runtime_ms")
            ),
            submissionId
        );
    }

    private int normalizeLimit(Integer limit) {
        if (limit == null) {
            return DEFAULT_LIMIT;
        }
        return Math.max(1, Math.min(limit, MAX_LIMIT));
    }

    private Instant instant(ResultSet rs, String columnName) throws SQLException {
        Object value = rs.getObject(columnName);
        if (value instanceof OffsetDateTime offsetDateTime) {
            return offsetDateTime.toInstant();
        }
        if (value instanceof Timestamp timestamp) {
            return timestamp.toInstant();
        }
        if (value instanceof Instant instant) {
            return instant;
        }
        throw new SQLException("Unsupported timestamp value for " + columnName + ": " + value);
    }
}
