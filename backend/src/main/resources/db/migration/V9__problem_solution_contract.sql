ALTER TABLE problems
  ADD COLUMN scenario TEXT NOT NULL DEFAULT '';

ALTER TABLE problems
  ADD COLUMN task TEXT NOT NULL DEFAULT '';

ALTER TABLE problems
  ADD COLUMN java_signature VARCHAR(255) NOT NULL DEFAULT '';

ALTER TABLE problems
  ADD COLUMN python_signature VARCHAR(255) NOT NULL DEFAULT '';

UPDATE problems
SET scenario = description
WHERE scenario = '';

UPDATE problems
SET task = description
WHERE task = '';
