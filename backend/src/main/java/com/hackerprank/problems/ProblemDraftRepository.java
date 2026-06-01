package com.hackerprank.problems;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.time.Instant;
import java.util.Optional;

import com.hackerprank.persistence.JdbcInstant;

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
            draft.getReferenceSolutions(),
            draft.getTopic(),
            draft.getValidationStatus(),
            draft.getGenerationMetadata()
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
        GenerationMetadata generationMetadata = problemRepository.findGenerationMetadataByProblemId(problemId)
            .orElseGet(GenerationMetadata::empty);

        return new ProblemDraft(
            rs.getString("id"),
            rs.getString("topic"),
            rs.getString("difficulty"),
            problem,
            problemRepository.findReferenceSolutionsByProblemId(problemId),
            rs.getString("validation_status"),
            generationMetadata,
            instant(rs, "created_at")
        );
    }

    private Instant instant(ResultSet rs, String columnName) throws SQLException {
        return JdbcInstant.from(rs, columnName);
    }
}
