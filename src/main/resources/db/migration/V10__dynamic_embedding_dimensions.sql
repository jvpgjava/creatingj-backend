DROP INDEX IF EXISTS idx_knowledge_chunks_embedding_hnsw;

ALTER TABLE knowledge_chunks
    ADD COLUMN embedding_dimensions INTEGER NOT NULL DEFAULT 768;

ALTER TABLE knowledge_chunks
    ALTER COLUMN embedding TYPE vector;

CREATE INDEX idx_knowledge_chunks_scope_dims
    ON knowledge_chunks (project_id, owner_user_id, embedding_version, embedding_dimensions);

CREATE INDEX idx_knowledge_chunks_hnsw_384
    ON knowledge_chunks USING hnsw ((embedding::vector(384)) vector_cosine_ops)
    WHERE embedding_dimensions = 384;

CREATE INDEX idx_knowledge_chunks_hnsw_768
    ON knowledge_chunks USING hnsw ((embedding::vector(768)) vector_cosine_ops)
    WHERE embedding_dimensions = 768;

CREATE INDEX idx_knowledge_chunks_hnsw_1024
    ON knowledge_chunks USING hnsw ((embedding::vector(1024)) vector_cosine_ops)
    WHERE embedding_dimensions = 1024;

CREATE INDEX idx_knowledge_chunks_hnsw_1536
    ON knowledge_chunks USING hnsw ((embedding::vector(1536)) vector_cosine_ops)
    WHERE embedding_dimensions = 1536;
