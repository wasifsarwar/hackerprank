CREATE TABLE tutor_messages (
  id VARCHAR(36) PRIMARY KEY,
  submission_id VARCHAR(36) NOT NULL REFERENCES submissions(id) ON DELETE CASCADE,
  role VARCHAR(20) NOT NULL,
  provider VARCHAR(80) NOT NULL,
  content TEXT NOT NULL,
  created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
  CONSTRAINT ck_tutor_messages_role CHECK (role IN ('user', 'assistant'))
);

CREATE INDEX idx_tutor_messages_submission_created ON tutor_messages (submission_id, created_at, id);
CREATE INDEX idx_tutor_messages_provider_created ON tutor_messages (provider, created_at DESC);
