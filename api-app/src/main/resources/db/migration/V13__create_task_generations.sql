CREATE TABLE IF NOT EXISTS task_generations (
    id UUID PRIMARY KEY,
    prompt TEXT NOT NULL,
    status TEXT NOT NULL,
    attempt INTEGER NOT NULL DEFAULT 0,
    max_attempts INTEGER NOT NULL,
    task_id UUID REFERENCES tasks(id),
    similar_tasks JSONB NOT NULL DEFAULT '[]'::jsonb,
    error_message TEXT,
    version INTEGER NOT NULL DEFAULT 1
);

CREATE INDEX IF NOT EXISTS task_generations_status_idx ON task_generations(status);
