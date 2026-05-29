package com.hackerprank.problems;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.time.Instant;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Locale;
import java.util.Optional;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

@Repository
public class GenerationAttemptRepository {
    private final JdbcTemplate jdbcTemplate;

    public GenerationAttemptRepository(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    @Transactional
    public GenerationAttempt save(GenerationAttempt attempt) {
        int updated = jdbcTemplate.update(
            """
                UPDATE generation_attempts
                SET problem_id = ?,
                    provider = ?,
                    model_id = ?,
                    prompt_version = ?,
                    topic = ?,
                    difficulty = ?,
                    outcome = ?,
                    feedback_notes = ?,
                    updated_at = CURRENT_TIMESTAMP
                WHERE id = ?
                """,
            attempt.getProblemId(),
            attempt.getProvider(),
            attempt.getModelId(),
            attempt.getPromptVersion(),
            attempt.getTopic(),
            attempt.getDifficulty(),
            normalizeOutcome(attempt.getOutcome()),
            attempt.getFeedbackNotes(),
            attempt.getId()
        );

        if (updated == 0) {
            jdbcTemplate.update(
                """
                    INSERT INTO generation_attempts (
                        id,
                        draft_id,
                        problem_id,
                        provider,
                        model_id,
                        prompt_version,
                        topic,
                        difficulty,
                        outcome,
                        feedback_notes,
                        created_at,
                        updated_at
                    )
                    VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
                    """,
                attempt.getId(),
                attempt.getDraftId(),
                attempt.getProblemId(),
                attempt.getProvider(),
                attempt.getModelId(),
                attempt.getPromptVersion(),
                attempt.getTopic(),
                attempt.getDifficulty(),
                normalizeOutcome(attempt.getOutcome()),
                attempt.getFeedbackNotes(),
                Timestamp.from(attempt.getCreatedAt()),
                Timestamp.from(attempt.getUpdatedAt())
            );
        }

        replaceFeedback(attempt.getId(), attempt.getFeedbackTags(), attempt.getFeedbackNotes());
        return findById(attempt.getId()).orElse(attempt);
    }

    public Optional<GenerationAttempt> findByDraftId(String draftId) {
        return jdbcTemplate.query(
            """
                SELECT id, draft_id, problem_id, provider, model_id, prompt_version, topic, difficulty,
                       outcome, feedback_notes, created_at, updated_at
                FROM generation_attempts
                WHERE draft_id = ?
                ORDER BY created_at DESC
                LIMIT 1
                """,
            (rs, rowNum) -> attemptFromRow(rs),
            draftId
        ).stream().findFirst();
    }

    public Optional<GenerationAttempt> findById(String id) {
        return jdbcTemplate.query(
            """
                SELECT id, draft_id, problem_id, provider, model_id, prompt_version, topic, difficulty,
                       outcome, feedback_notes, created_at, updated_at
                FROM generation_attempts
                WHERE id = ?
                """,
            (rs, rowNum) -> attemptFromRow(rs),
            id
        ).stream().findFirst();
    }

    @Transactional
    public Optional<GenerationAttempt> replaceFeedbackByDraftId(String draftId, List<String> tags, String notes) {
        Optional<GenerationAttempt> attempt = findByDraftId(draftId);
        attempt.ifPresent(value -> replaceFeedback(value.getId(), tags, notes));
        return findByDraftId(draftId);
    }

    @Transactional
    public void updateOutcomeByDraftId(String draftId, String outcome) {
        jdbcTemplate.update(
            """
                UPDATE generation_attempts
                SET outcome = ?, updated_at = CURRENT_TIMESTAMP
                WHERE draft_id = ?
                """,
            normalizeOutcome(outcome),
            draftId
        );
    }

    @Transactional
    public void replaceFeedback(String attemptId, List<String> tags, String notes) {
        jdbcTemplate.update(
            """
                UPDATE generation_attempts
                SET feedback_notes = ?, updated_at = CURRENT_TIMESTAMP
                WHERE id = ?
                """,
            normalizeNotes(notes),
            attemptId
        );
        jdbcTemplate.update("DELETE FROM generation_attempt_feedback_tags WHERE attempt_id = ?", attemptId);

        for (String tag : normalizeTags(tags)) {
            jdbcTemplate.update(
                """
                    INSERT INTO generation_attempt_feedback_tags (attempt_id, tag)
                    VALUES (?, ?)
                    """,
                attemptId,
                tag
            );
        }
    }

    private GenerationAttempt attemptFromRow(ResultSet rs) throws SQLException {
        String id = rs.getString("id");
        return new GenerationAttempt(
            id,
            rs.getString("draft_id"),
            rs.getString("problem_id"),
            rs.getString("provider"),
            rs.getString("model_id"),
            rs.getString("prompt_version"),
            rs.getString("topic"),
            rs.getString("difficulty"),
            rs.getString("outcome"),
            tagsForAttempt(id),
            rs.getString("feedback_notes"),
            instant(rs, "created_at"),
            instant(rs, "updated_at")
        );
    }

    private List<String> tagsForAttempt(String attemptId) {
        return jdbcTemplate.query(
            """
                SELECT tag
                FROM generation_attempt_feedback_tags
                WHERE attempt_id = ?
                ORDER BY tag
                """,
            (rs, rowNum) -> rs.getString("tag"),
            attemptId
        );
    }

    private List<String> normalizeTags(List<String> tags) {
        if (tags == null || tags.isEmpty()) {
            return List.of();
        }

        return tags.stream()
            .map(tag -> tag == null ? "" : tag.trim())
            .filter(tag -> !tag.isBlank())
            .distinct()
            .limit(12)
            .toList();
    }

    private String normalizeNotes(String notes) {
        String normalized = notes == null ? "" : notes.trim();
        return normalized.length() > 2_000 ? normalized.substring(0, 2_000) : normalized;
    }

    private String normalizeOutcome(String outcome) {
        String normalized = outcome == null ? "" : outcome.trim().toUpperCase(Locale.ROOT);
        return switch (normalized) {
            case "PUBLISHED", "DISCARDED", "REGENERATED" -> normalized;
            default -> "DRAFTED";
        };
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
