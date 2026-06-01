CREATE INDEX idx_generation_attempts_draft_created
ON generation_attempts (draft_id, created_at DESC, id);
