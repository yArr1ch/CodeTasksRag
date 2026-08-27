ALTER TABLE submissions
    ADD COLUMN IF NOT EXISTS execution_mode TEXT NOT NULL DEFAULT 'STANDARD';

UPDATE submissions submission
SET execution_mode = 'REFERENCE_ORACLE'
FROM tasks task
WHERE submission.task_id = task.id
  AND submission.status = 'PASSED'
  AND EXISTS (
      SELECT 1
      FROM jsonb_array_elements_text(task.reference_solutions) reference_solution
      WHERE reference_solution = submission.source_code
  );
