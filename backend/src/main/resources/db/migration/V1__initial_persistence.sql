CREATE TABLE problems (
  id VARCHAR(120) PRIMARY KEY,
  title VARCHAR(255) NOT NULL,
  difficulty VARCHAR(20) NOT NULL,
  description TEXT NOT NULL,
  input_format TEXT NOT NULL,
  output_format TEXT NOT NULL,
  publication_status VARCHAR(20) NOT NULL,
  source_type VARCHAR(30) NOT NULL,
  sort_order INTEGER NOT NULL,
  created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
  updated_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
  CONSTRAINT ck_problems_difficulty CHECK (difficulty IN ('Easy', 'Medium', 'Hard')),
  CONSTRAINT ck_problems_publication_status CHECK (publication_status IN ('DRAFT', 'PUBLISHED'))
);

CREATE INDEX idx_problems_status_sort ON problems (publication_status, sort_order, id);
CREATE INDEX idx_problems_status_difficulty ON problems (publication_status, difficulty);
CREATE INDEX idx_problems_source_type ON problems (source_type);

CREATE TABLE problem_tags (
  problem_id VARCHAR(120) NOT NULL REFERENCES problems(id) ON DELETE CASCADE,
  display_order INTEGER NOT NULL,
  tag VARCHAR(80) NOT NULL,
  PRIMARY KEY (problem_id, display_order)
);

CREATE INDEX idx_problem_tags_tag ON problem_tags (tag);

CREATE TABLE problem_constraints (
  problem_id VARCHAR(120) NOT NULL REFERENCES problems(id) ON DELETE CASCADE,
  display_order INTEGER NOT NULL,
  constraint_text TEXT NOT NULL,
  PRIMARY KEY (problem_id, display_order)
);

CREATE TABLE problem_examples (
  problem_id VARCHAR(120) NOT NULL REFERENCES problems(id) ON DELETE CASCADE,
  display_order INTEGER NOT NULL,
  input_text TEXT NOT NULL,
  output_text TEXT NOT NULL,
  explanation TEXT NOT NULL,
  PRIMARY KEY (problem_id, display_order)
);

CREATE TABLE problem_test_cases (
  problem_id VARCHAR(120) NOT NULL REFERENCES problems(id) ON DELETE CASCADE,
  display_order INTEGER NOT NULL,
  name VARCHAR(120) NOT NULL,
  input_text TEXT NOT NULL,
  expected_output TEXT NOT NULL,
  hidden BOOLEAN NOT NULL,
  PRIMARY KEY (problem_id, display_order)
);

CREATE INDEX idx_problem_test_cases_problem_hidden ON problem_test_cases (problem_id, hidden, display_order);

CREATE TABLE problem_starter_code (
  problem_id VARCHAR(120) NOT NULL REFERENCES problems(id) ON DELETE CASCADE,
  language VARCHAR(30) NOT NULL,
  code TEXT NOT NULL,
  PRIMARY KEY (problem_id, language)
);

CREATE TABLE problem_private_artifacts (
  problem_id VARCHAR(120) PRIMARY KEY REFERENCES problems(id) ON DELETE CASCADE,
  reference_solution TEXT,
  generator_topic VARCHAR(255),
  validation_status VARCHAR(40),
  created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
  updated_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX idx_problem_private_artifacts_topic ON problem_private_artifacts (generator_topic);
CREATE INDEX idx_problem_private_artifacts_validation_status ON problem_private_artifacts (validation_status);

CREATE TABLE problem_drafts (
  id VARCHAR(120) PRIMARY KEY,
  problem_id VARCHAR(120) NOT NULL UNIQUE REFERENCES problems(id) ON DELETE CASCADE,
  topic VARCHAR(255) NOT NULL,
  difficulty VARCHAR(20) NOT NULL,
  validation_status VARCHAR(40) NOT NULL,
  created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
  CONSTRAINT ck_problem_drafts_difficulty CHECK (difficulty IN ('Easy', 'Medium', 'Hard'))
);

CREATE INDEX idx_problem_drafts_created_at ON problem_drafts (created_at DESC);
CREATE INDEX idx_problem_drafts_topic ON problem_drafts (topic);

CREATE TABLE submissions (
  id VARCHAR(36) PRIMARY KEY,
  problem_id VARCHAR(120) REFERENCES problems(id) ON DELETE SET NULL,
  problem_title VARCHAR(255) NOT NULL,
  problem_difficulty VARCHAR(20) NOT NULL,
  language VARCHAR(30) NOT NULL,
  code TEXT NOT NULL,
  run_hidden_tests BOOLEAN NOT NULL,
  status VARCHAR(40) NOT NULL,
  passed_count INTEGER NOT NULL,
  total_count INTEGER NOT NULL,
  compile_output TEXT NOT NULL,
  created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX idx_submissions_problem_created ON submissions (problem_id, created_at DESC);
CREATE INDEX idx_submissions_created_at ON submissions (created_at DESC);
CREATE INDEX idx_submissions_status_created ON submissions (status, created_at DESC);
CREATE INDEX idx_submissions_language_created ON submissions (language, created_at DESC);

CREATE TABLE submission_test_results (
  submission_id VARCHAR(36) NOT NULL REFERENCES submissions(id) ON DELETE CASCADE,
  display_order INTEGER NOT NULL,
  test_name VARCHAR(120) NOT NULL,
  hidden BOOLEAN NOT NULL,
  passed BOOLEAN NOT NULL,
  expected_output TEXT,
  actual_output TEXT NOT NULL,
  stderr TEXT NOT NULL,
  timed_out BOOLEAN NOT NULL,
  exit_code INTEGER NOT NULL,
  runtime_ms BIGINT NOT NULL,
  PRIMARY KEY (submission_id, display_order)
);

CREATE INDEX idx_submission_test_results_submission ON submission_test_results (submission_id, display_order);
CREATE INDEX idx_submission_test_results_passed ON submission_test_results (passed);
