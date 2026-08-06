CREATE EXTENSION IF NOT EXISTS vector;

ALTER TABLE tasks
    ADD COLUMN IF NOT EXISTS embedding vector(768);

CREATE INDEX IF NOT EXISTS tasks_embedding_idx
    ON tasks USING hnsw (embedding vector_cosine_ops)
    WHERE embedding IS NOT NULL;
