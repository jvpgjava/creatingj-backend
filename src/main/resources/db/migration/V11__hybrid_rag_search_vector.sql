ALTER TABLE knowledge_chunks
    ADD COLUMN IF NOT EXISTS search_vector tsvector
    GENERATED ALWAYS AS (
        to_tsvector('portuguese', coalesce(content, ''))
        || to_tsvector('simple', coalesce(content, ''))
    ) STORED;

CREATE INDEX IF NOT EXISTS idx_knowledge_chunks_search_vector_gin
    ON knowledge_chunks
    USING gin (search_vector);
