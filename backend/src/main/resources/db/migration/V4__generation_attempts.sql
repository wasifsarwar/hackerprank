CREATE TABLE generation_attempts (
  id VARCHAR(120) PRIMARY KEY,
  draft_id VARCHAR(120) NOT NULL,
  problem_id VARCHAR(120) NOT NULL,
  provider VARCHAR(80) NOT NULL,
  model_id VARCHAR(120) NOT NULL,
  prompt_version VARCHAR(120) NOT NULL,
  topic VARCHAR(255) NOT NULL,
  difficulty VARCHAR(20) NOT NULL,
  outcome VARCHAR(40) NOT NULL,
  feedback_notes TEXT NOT NULL DEFAULT '',
  created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
  updated_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
  CONSTRAINT ck_generation_attempts_difficulty CHECK (difficulty IN ('Easy', 'Medium', 'Hard')),
  CONSTRAINT ck_generation_attempts_outcome CHECK (outcome IN ('DRAFTED', 'PUBLISHED', 'DISCARDED', 'REGENERATED'))
);

CREATE INDEX idx_generation_attempts_draft_id ON generation_attempts (draft_id);
CREATE INDEX idx_generation_attempts_problem_id ON generation_attempts (problem_id);
CREATE INDEX idx_generation_attempts_provider_created ON generation_attempts (provider, created_at DESC);
CREATE INDEX idx_generation_attempts_topic_difficulty_created ON generation_attempts (topic, difficulty, created_at DESC);
CREATE INDEX idx_generation_attempts_outcome_created ON generation_attempts (outcome, created_at DESC);

CREATE TABLE generation_attempt_feedback_tags (
  attempt_id VARCHAR(120) NOT NULL REFERENCES generation_attempts(id) ON DELETE CASCADE,
  tag VARCHAR(80) NOT NULL,
  PRIMARY KEY (attempt_id, tag)
);

CREATE INDEX idx_generation_attempt_feedback_tags_tag ON generation_attempt_feedback_tags (tag);
