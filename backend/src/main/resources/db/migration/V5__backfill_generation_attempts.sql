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
SELECT
  CONCAT('attempt-backfill-', LEFT(d.id, 100)),
  d.id,
  d.problem_id,
  COALESCE(NULLIF(p.generator_provider, ''), 'deterministic'),
  COALESCE(NULLIF(p.generator_model_id, ''), 'template-v1'),
  COALESCE(NULLIF(p.generator_prompt_version, ''), 'deterministic-v1'),
  d.topic,
  d.difficulty,
  'DRAFTED',
  '',
  d.created_at,
  CURRENT_TIMESTAMP
FROM problem_drafts d
LEFT JOIN problem_private_artifacts p ON p.problem_id = d.problem_id
WHERE NOT EXISTS (
  SELECT 1
  FROM generation_attempts existing
  WHERE existing.draft_id = d.id
);
