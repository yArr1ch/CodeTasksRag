ALTER TABLE tasks
    ADD COLUMN IF NOT EXISTS reference_solutions JSONB NOT NULL DEFAULT '[]'::jsonb;

UPDATE tasks
SET reference_solutions = jsonb_build_array(reference_solution)
WHERE reference_solution IS NOT NULL
  AND reference_solution <> ''
  AND reference_solutions = '[]'::jsonb;

ALTER TABLE tasks
    DROP COLUMN IF EXISTS reference_solution;
