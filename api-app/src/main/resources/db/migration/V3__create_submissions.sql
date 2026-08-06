CREATE TABLE IF NOT EXISTS submissions (
    id UUID PRIMARY KEY,
    task_id UUID NOT NULL,
    source_code TEXT NOT NULL,
    status TEXT NOT NULL,
    passed_tests INTEGER NOT NULL DEFAULT 0,
    total_tests INTEGER NOT NULL DEFAULT 0,
    error TEXT
);

CREATE INDEX IF NOT EXISTS submissions_task_id_idx ON submissions(task_id);
