CREATE TABLE projects (
    id UUID PRIMARY KEY,
    owner_user_id UUID NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    name VARCHAR(120) NOT NULL,
    slug VARCHAR(120) NOT NULL,
    description VARCHAR(500) NOT NULL DEFAULT '',
    created_at TIMESTAMPTZ NOT NULL,
    updated_at TIMESTAMPTZ NOT NULL,
    CONSTRAINT uq_projects_owner_slug UNIQUE (owner_user_id, slug)
);

CREATE INDEX idx_projects_owner_created ON projects (owner_user_id, created_at DESC);

CREATE TABLE knowledge_sources (
    id UUID PRIMARY KEY,
    project_id UUID NOT NULL REFERENCES projects(id) ON DELETE CASCADE,
    owner_user_id UUID NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    source_path VARCHAR(260) NOT NULL,
    title VARCHAR(160) NOT NULL,
    content_hash VARCHAR(64) NOT NULL,
    embedding_version VARCHAR(80) NOT NULL,
    status VARCHAR(24) NOT NULL,
    chunk_count INTEGER NOT NULL DEFAULT 0,
    created_at TIMESTAMPTZ NOT NULL,
    updated_at TIMESTAMPTZ NOT NULL,
    CONSTRAINT uq_knowledge_sources_project_path UNIQUE (project_id, source_path)
);

CREATE INDEX idx_knowledge_sources_project_owner ON knowledge_sources (project_id, owner_user_id, updated_at DESC);

CREATE TABLE knowledge_chunks (
    id UUID PRIMARY KEY,
    source_id UUID NOT NULL REFERENCES knowledge_sources(id) ON DELETE CASCADE,
    project_id UUID NOT NULL REFERENCES projects(id) ON DELETE CASCADE,
    owner_user_id UUID NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    chunk_index INTEGER NOT NULL,
    content TEXT NOT NULL,
    content_hash VARCHAR(64) NOT NULL,
    token_estimate INTEGER NOT NULL,
    embedding_version VARCHAR(80) NOT NULL,
    embedding vector(384) NOT NULL,
    created_at TIMESTAMPTZ NOT NULL,
    CONSTRAINT uq_knowledge_chunks_source_index UNIQUE (source_id, chunk_index)
);

CREATE INDEX idx_knowledge_chunks_scope ON knowledge_chunks (project_id, owner_user_id, embedding_version);
CREATE INDEX idx_knowledge_chunks_source ON knowledge_chunks (source_id);
CREATE INDEX idx_knowledge_chunks_embedding_hnsw ON knowledge_chunks USING hnsw (embedding vector_cosine_ops);

CREATE TABLE knowledge_index_jobs (
    id UUID PRIMARY KEY,
    project_id UUID NOT NULL REFERENCES projects(id) ON DELETE CASCADE,
    owner_user_id UUID NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    source_id UUID REFERENCES knowledge_sources(id) ON DELETE SET NULL,
    source_path VARCHAR(260) NOT NULL,
    operation VARCHAR(24) NOT NULL,
    status VARCHAR(24) NOT NULL,
    chunks_indexed INTEGER NOT NULL DEFAULT 0,
    error_code VARCHAR(64),
    created_at TIMESTAMPTZ NOT NULL,
    completed_at TIMESTAMPTZ
);

CREATE INDEX idx_knowledge_index_jobs_scope ON knowledge_index_jobs (project_id, owner_user_id, created_at DESC);
