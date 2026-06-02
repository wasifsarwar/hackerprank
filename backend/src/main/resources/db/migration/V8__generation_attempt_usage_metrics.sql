ALTER TABLE generation_attempts
  ADD COLUMN prompt_hash VARCHAR(64) NOT NULL DEFAULT '';

ALTER TABLE generation_attempts
  ADD COLUMN response_hash VARCHAR(64) NOT NULL DEFAULT '';

ALTER TABLE generation_attempts
  ADD COLUMN prompt_char_count INTEGER NOT NULL DEFAULT 0;

ALTER TABLE generation_attempts
  ADD COLUMN response_char_count INTEGER NOT NULL DEFAULT 0;

ALTER TABLE generation_attempts
  ADD COLUMN estimated_prompt_tokens INTEGER NOT NULL DEFAULT 0;

ALTER TABLE generation_attempts
  ADD COLUMN estimated_response_tokens INTEGER NOT NULL DEFAULT 0;

CREATE INDEX idx_generation_attempts_provider_prompt_hash_created
ON generation_attempts (provider, prompt_hash, created_at DESC);
