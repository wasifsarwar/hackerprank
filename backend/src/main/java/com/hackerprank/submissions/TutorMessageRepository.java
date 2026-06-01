package com.hackerprank.submissions;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

import com.hackerprank.persistence.JdbcInstant;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

@Repository
class TutorMessageRepository {
    private static final int DEFAULT_LIMIT = 30;
    private static final int MAX_LIMIT = 60;

    private final JdbcTemplate jdbcTemplate;

    TutorMessageRepository(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    TutorMessage save(String submissionId, String role, String provider, String content) {
        String id = UUID.randomUUID().toString();
        Instant createdAt = Instant.now();
        jdbcTemplate.update(
            """
                INSERT INTO tutor_messages (id, submission_id, role, provider, content, created_at)
                VALUES (?, ?, ?, ?, ?, ?)
                """,
            id,
            submissionId,
            role,
            provider,
            content == null ? "" : content,
            Timestamp.from(createdAt)
        );
        return new TutorMessage(id, submissionId, role, provider, content == null ? "" : content, createdAt);
    }

    List<TutorMessage> findBySubmissionId(String submissionId) {
        return findBySubmissionId(submissionId, DEFAULT_LIMIT);
    }

    List<TutorMessage> findBySubmissionId(String submissionId, Integer limit) {
        return jdbcTemplate.query(
            """
                SELECT id, submission_id, role, provider, content, created_at
                FROM (
                  SELECT id, submission_id, role, provider, content, created_at
                  FROM tutor_messages
                  WHERE submission_id = ?
                  ORDER BY created_at DESC, id DESC
                  LIMIT ?
                ) recent
                ORDER BY created_at, id
                """,
            this::messageFromRow,
            submissionId,
            normalizeLimit(limit)
        );
    }

    private TutorMessage messageFromRow(ResultSet rs, int rowNum) throws SQLException {
        return new TutorMessage(
            rs.getString("id"),
            rs.getString("submission_id"),
            rs.getString("role"),
            rs.getString("provider"),
            rs.getString("content"),
            instant(rs, "created_at")
        );
    }

    private int normalizeLimit(Integer limit) {
        if (limit == null) {
            return DEFAULT_LIMIT;
        }
        return Math.max(1, Math.min(limit, MAX_LIMIT));
    }

    private Instant instant(ResultSet rs, String columnName) throws SQLException {
        return JdbcInstant.from(rs, columnName);
    }
}
