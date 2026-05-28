package com.hackerprank.problems;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;

import org.springframework.stereotype.Repository;

@Repository
public class ProblemDraftRepository {
    private final Map<String, ProblemDraft> drafts = new LinkedHashMap<>();

    public synchronized ProblemDraft save(ProblemDraft draft) {
        if (draft == null || draft.getId() == null || draft.getId().isBlank()) {
            throw new IllegalArgumentException("Draft id is required");
        }

        drafts.put(draft.getId(), draft);
        return draft;
    }

    public synchronized Optional<ProblemDraft> findById(String id) {
        return Optional.ofNullable(drafts.get(id));
    }

    public synchronized void deleteById(String id) {
        drafts.remove(id);
    }

    public synchronized boolean existsByProblemId(String problemId) {
        return drafts.values().stream()
            .anyMatch(draft -> draft.getProblem().getId().equals(problemId));
    }
}

