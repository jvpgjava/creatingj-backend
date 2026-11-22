CREATE TABLE user_quotas (
    user_id UUID PRIMARY KEY,
    daily_token_limit BIGINT NOT NULL,
    daily_request_limit INT NOT NULL,
    max_concurrent_generations INT NOT NULL,
    updated_by UUID,
    updated_at TIMESTAMPTZ NOT NULL,
    CONSTRAINT fk_user_quotas_user FOREIGN KEY (user_id) REFERENCES users (id) ON DELETE CASCADE,
    CONSTRAINT chk_user_quotas_tokens CHECK (daily_token_limit > 0),
    CONSTRAINT chk_user_quotas_requests CHECK (daily_request_limit > 0),
    CONSTRAINT chk_user_quotas_concurrency CHECK (max_concurrent_generations > 0)
);

CREATE TABLE usage_records (
    id UUID PRIMARY KEY,
    user_id UUID NOT NULL,
    conversation_id UUID,
    generation_id UUID,
    model_id VARCHAR(120) NOT NULL,
    source VARCHAR(32) NOT NULL,
    prompt_tokens INT NOT NULL,
    completion_tokens INT NOT NULL,
    total_tokens INT NOT NULL,
    estimated BOOLEAN NOT NULL,
    latency_ms BIGINT NOT NULL,
    outcome VARCHAR(32) NOT NULL,
    recorded_at TIMESTAMPTZ NOT NULL,
    CONSTRAINT fk_usage_records_user FOREIGN KEY (user_id) REFERENCES users (id) ON DELETE CASCADE,
    CONSTRAINT fk_usage_records_conversation FOREIGN KEY (conversation_id) REFERENCES conversations (id) ON DELETE SET NULL,
    CONSTRAINT fk_usage_records_generation FOREIGN KEY (generation_id) REFERENCES generations (id) ON DELETE SET NULL,
    CONSTRAINT chk_usage_records_source CHECK (source IN ('CHAT_SYNC', 'CHAT_STREAM')),
    CONSTRAINT chk_usage_records_outcome CHECK (outcome IN ('COMPLETED', 'CANCELLED', 'FAILED')),
    CONSTRAINT chk_usage_records_tokens CHECK (prompt_tokens >= 0 AND completion_tokens >= 0 AND total_tokens >= 0),
    CONSTRAINT chk_usage_records_latency CHECK (latency_ms >= 0)
);

CREATE INDEX idx_usage_records_user_recorded ON usage_records (user_id, recorded_at DESC);
CREATE INDEX idx_usage_records_recorded ON usage_records (recorded_at DESC);
