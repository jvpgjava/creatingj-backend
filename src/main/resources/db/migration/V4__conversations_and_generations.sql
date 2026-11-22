CREATE TABLE conversations (
    id UUID PRIMARY KEY,
    owner_user_id UUID NOT NULL,
    title VARCHAR(160) NOT NULL,
    model_id VARCHAR(120) NOT NULL,
    created_at TIMESTAMPTZ NOT NULL,
    updated_at TIMESTAMPTZ NOT NULL,
    archived_at TIMESTAMPTZ,
    CONSTRAINT fk_conversations_owner FOREIGN KEY (owner_user_id) REFERENCES users (id) ON DELETE CASCADE
);

CREATE INDEX idx_conversations_owner_updated ON conversations (owner_user_id, updated_at DESC);

CREATE TABLE messages (
    id UUID PRIMARY KEY,
    conversation_id UUID NOT NULL,
    role VARCHAR(16) NOT NULL,
    content TEXT NOT NULL,
    sequence_num INT NOT NULL,
    created_at TIMESTAMPTZ NOT NULL,
    CONSTRAINT fk_messages_conversation FOREIGN KEY (conversation_id) REFERENCES conversations (id) ON DELETE CASCADE,
    CONSTRAINT uk_messages_conversation_seq UNIQUE (conversation_id, sequence_num),
    CONSTRAINT chk_messages_role CHECK (role IN ('SYSTEM', 'USER', 'ASSISTANT'))
);

CREATE INDEX idx_messages_conversation_seq ON messages (conversation_id, sequence_num ASC);

CREATE TABLE generations (
    id UUID PRIMARY KEY,
    conversation_id UUID NOT NULL,
    owner_user_id UUID NOT NULL,
    user_message_id UUID NOT NULL,
    assistant_message_id UUID,
    model_id VARCHAR(120) NOT NULL,
    status VARCHAR(24) NOT NULL,
    prompt_tokens INT,
    completion_tokens INT,
    total_tokens INT,
    error_code VARCHAR(64),
    created_at TIMESTAMPTZ NOT NULL,
    completed_at TIMESTAMPTZ,
    CONSTRAINT fk_generations_conversation FOREIGN KEY (conversation_id) REFERENCES conversations (id) ON DELETE CASCADE,
    CONSTRAINT fk_generations_owner FOREIGN KEY (owner_user_id) REFERENCES users (id) ON DELETE CASCADE,
    CONSTRAINT fk_generations_user_msg FOREIGN KEY (user_message_id) REFERENCES messages (id) ON DELETE CASCADE,
    CONSTRAINT fk_generations_assistant_msg FOREIGN KEY (assistant_message_id) REFERENCES messages (id) ON DELETE SET NULL,
    CONSTRAINT chk_generations_status CHECK (status IN ('PENDING', 'STREAMING', 'COMPLETED', 'CANCELLED', 'FAILED'))
);

CREATE INDEX idx_generations_conversation_created ON generations (conversation_id, created_at DESC);
CREATE INDEX idx_generations_owner_status ON generations (owner_user_id, status);
