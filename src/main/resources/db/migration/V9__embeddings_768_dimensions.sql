DROP INDEX IF EXISTS idx_knowledge_chunks_embedding_hnsw;

DELETE FROM knowledge_chunks;

ALTER TABLE knowledge_chunks
    ALTER COLUMN embedding TYPE vector(768);

CREATE INDEX idx_knowledge_chunks_embedding_hnsw
    ON knowledge_chunks USING hnsw (embedding vector_cosine_ops);
