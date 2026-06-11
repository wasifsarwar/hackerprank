ALTER TABLE problems DROP CONSTRAINT ck_problems_publication_status;
ALTER TABLE problems ADD CONSTRAINT ck_problems_publication_status
  CHECK (publication_status IN ('DRAFT', 'PUBLISHED', 'ARCHIVED'));
