CREATE TABLE problem_reference_solutions (
  problem_id VARCHAR(120) NOT NULL REFERENCES problems(id) ON DELETE CASCADE,
  language VARCHAR(30) NOT NULL,
  code TEXT NOT NULL,
  PRIMARY KEY (problem_id, language)
);

INSERT INTO problem_reference_solutions (problem_id, language, code)
SELECT problem_id, 'python', reference_solution
FROM problem_private_artifacts
WHERE reference_solution IS NOT NULL AND reference_solution <> '';

ALTER TABLE problem_private_artifacts ADD COLUMN generator_provider VARCHAR(80) NOT NULL DEFAULT 'deterministic';
ALTER TABLE problem_private_artifacts ADD COLUMN generator_model_id VARCHAR(120) NOT NULL DEFAULT 'template-v1';
ALTER TABLE problem_private_artifacts ADD COLUMN generator_prompt_version VARCHAR(80) NOT NULL DEFAULT 'deterministic-v1';
ALTER TABLE problem_private_artifacts ADD COLUMN generator_prompt_text TEXT NOT NULL DEFAULT '';
ALTER TABLE problem_private_artifacts ADD COLUMN generator_parameters_json TEXT NOT NULL DEFAULT '';
ALTER TABLE problem_private_artifacts ADD COLUMN validation_errors TEXT NOT NULL DEFAULT '';
ALTER TABLE problem_private_artifacts ADD COLUMN validation_summary TEXT NOT NULL DEFAULT '';
ALTER TABLE problem_private_artifacts ADD COLUMN intended_technique TEXT NOT NULL DEFAULT '';

CREATE INDEX idx_problem_private_artifacts_provider ON problem_private_artifacts (generator_provider);
CREATE INDEX idx_problem_private_artifacts_model ON problem_private_artifacts (generator_model_id);
