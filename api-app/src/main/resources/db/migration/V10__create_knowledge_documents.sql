CREATE TABLE IF NOT EXISTS knowledge_documents (
    id UUID DEFAULT uuid_generate_v4() PRIMARY KEY,
    title TEXT NOT NULL,
    content TEXT NOT NULL,
    source TEXT NOT NULL,
    topic TEXT NOT NULL
);
