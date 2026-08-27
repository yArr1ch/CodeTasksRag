CREATE UNIQUE INDEX IF NOT EXISTS knowledge_documents_title_source_unique_idx
    ON knowledge_documents (lower(trim(title)), lower(trim(source)));
