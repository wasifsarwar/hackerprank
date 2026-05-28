package com.hackerprank.problems;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.time.Instant;
import java.time.OffsetDateTime;
import java.util.Optional;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

@Repository
public class ProblemDraftRepository {
    private final JdbcTemplate jdbcTemplate;
    private final ProblemRepository problemRepository;

    public ProblemDraftRepository(JdbcTemplate jdbcTemplate, ProblemRepository problemRepository) {
        this.jdbcTemplate = jdbcTemplate;
        this.problemRepository = problemRepository;
    }

    @Transactional
    public ProblemDraft save(ProblemDraft draft) {
        if (draft == null || draft.getId() == null || draft.getId().isBlank()) {
            throw new IllegalArgumentException("Draft id is required");
        }

        problemRepository.saveDraftProblem(draft.getProblem());
        problemRepository.savePrivateArtifacts(
            draft.getProblem().getId(),
            draft.getReferenceSolution(),
            draft.getTopic(),
            draft.getValidationStatus()
        );

        int updated = jdbcTemplate.update(
            """
                UPDATE problem_drafts
                SET topic = ?, difficulty = ?, validation_status = ?
                WHERE id = ?
                """,
            draft.getTopic(),
            draft.getDifficulty(),
            draft.getValidationStatus(),
            draft.getId()
        );

        if (updated == 0) {
            jdbcTemplate.update(
                """
                    INSERT INTO problem_drafts (
                        id,
                        problem_id,
                        topic,
                        difficulty,
                        validation_status,
                        created_at
                    )
                    VALUES (?, ?, ?, ?, ?, ?)
                    """,
                draft.getId(),
                draft.getProblem().getId(),
                draft.getTopic(),
                draft.getDifficulty(),
                draft.getValidationStatus(),
                Timestamp.from(draft.getCreatedAt())
            );
        }

        return draft;
    }

    public Optional<ProblemDraft> findById(String id) {
        return jdbcTemplate.query(
            """
                SELECT id, problem_id, topic, difficulty, validation_status, created_at
                FROM problem_drafts
                WHERE id = ?
                """,
            (rs, rowNum) -> draftFromRow(rs),
            id
        ).stream().findFirst();
    }

    @Transactional
    public void deleteById(String id) {
        findProblemIdByDraftId(id).ifPresent(problemRepository::deleteById);
    }

    @Transactional
    public void publishById(String id) {
        Optional<String> problemId = findProblemIdByDraftId(id);
        problemId.ifPresent(value -> {
            problemRepository.publishById(value);
            deleteDraftMetadataById(id);
        });
    }

    public boolean existsByProblemId(String problemId) {
        return problemRepository.existsAnyById(problemId);
    }

    void deleteDraftMetadataById(String id) {
        jdbcTemplate.update("DELETE FROM problem_drafts WHERE id = ?", id);
    }

    private Optional<String> findProblemIdByDraftId(String id) {
        return jdbcTemplate.query(
            "SELECT problem_id FROM problem_drafts WHERE id = ?",
            (rs, rowNum) -> rs.getString("problem_id"),
            id
        ).stream().findFirst();
    }

    private ProblemDraft draftFromRow(ResultSet rs) throws SQLException {
        String problemId = rs.getString("problem_id");
        Problem problem = problemRepository.findAnyById(problemId)
            .orElseThrow(() -> new IllegalStateException("Draft problem not found: " + problemId));
        String referenceSolution = problemRepository.findReferenceSolutionByProblemId(problemId).orElse("");

        return new ProblemDraft(
            rs.getString("id"),
            rs.getString("topic"),
            rs.getString("difficulty"),
            problem,
            referenceSolution,
            rs.getString("validation_status"),
            instant(rs, "created_at")
        );
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
