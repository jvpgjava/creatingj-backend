CREATE TABLE devices (
    id UUID PRIMARY KEY,
    owner_user_id UUID NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    name VARCHAR(120) NOT NULL,
    platform VARCHAR(40) NOT NULL,
    status VARCHAR(24) NOT NULL,
    last_heartbeat_at TIMESTAMPTZ,
    created_at TIMESTAMPTZ NOT NULL,
    revoked_at TIMESTAMPTZ
);

CREATE INDEX idx_devices_owner_created ON devices (owner_user_id, created_at DESC);

CREATE TABLE device_pairing_challenges (
    id UUID PRIMARY KEY,
    owner_user_id UUID NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    device_name VARCHAR(120) NOT NULL,
    platform VARCHAR(40) NOT NULL,
    code_hash VARCHAR(64) NOT NULL UNIQUE,
    expires_at TIMESTAMPTZ NOT NULL,
    consumed_at TIMESTAMPTZ,
    created_at TIMESTAMPTZ NOT NULL
);

CREATE TABLE device_credentials (
    id UUID PRIMARY KEY,
    device_id UUID NOT NULL REFERENCES devices(id) ON DELETE CASCADE,
    owner_user_id UUID NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    token_hash VARCHAR(64) NOT NULL UNIQUE,
    created_at TIMESTAMPTZ NOT NULL,
    revoked_at TIMESTAMPTZ
);

CREATE INDEX idx_device_credentials_device ON device_credentials (device_id);

CREATE TABLE remote_tasks (
    id UUID PRIMARY KEY,
    owner_user_id UUID NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    project_id UUID REFERENCES projects(id) ON DELETE SET NULL,
    device_id UUID NOT NULL REFERENCES devices(id) ON DELETE CASCADE,
    idempotency_key VARCHAR(120),
    title VARCHAR(160) NOT NULL,
    instruction TEXT NOT NULL,
    status VARCHAR(32) NOT NULL,
    lease_expires_at TIMESTAMPTZ,
    result_summary TEXT,
    error_code VARCHAR(64),
    created_at TIMESTAMPTZ NOT NULL,
    updated_at TIMESTAMPTZ NOT NULL,
    completed_at TIMESTAMPTZ
);

CREATE UNIQUE INDEX uq_remote_tasks_idempotency
    ON remote_tasks (owner_user_id, device_id, idempotency_key)
    WHERE idempotency_key IS NOT NULL;

CREATE INDEX idx_remote_tasks_owner_created ON remote_tasks (owner_user_id, created_at DESC);
CREATE INDEX idx_remote_tasks_device_status ON remote_tasks (device_id, status, created_at ASC);

CREATE TABLE task_events (
    id UUID PRIMARY KEY,
    task_id UUID NOT NULL REFERENCES remote_tasks(id) ON DELETE CASCADE,
    event_key VARCHAR(120) NOT NULL,
    event_type VARCHAR(40) NOT NULL,
    message VARCHAR(1000) NOT NULL,
    created_at TIMESTAMPTZ NOT NULL,
    CONSTRAINT uq_task_events_dedupe UNIQUE (task_id, event_key)
);

CREATE INDEX idx_task_events_task_created ON task_events (task_id, created_at ASC);

CREATE TABLE tool_approvals (
    id UUID PRIMARY KEY,
    task_id UUID NOT NULL REFERENCES remote_tasks(id) ON DELETE CASCADE,
    owner_user_id UUID NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    device_id UUID NOT NULL REFERENCES devices(id) ON DELETE CASCADE,
    tool_name VARCHAR(40) NOT NULL,
    target_path VARCHAR(260),
    command_text VARCHAR(1000),
    diff_preview TEXT,
    status VARCHAR(24) NOT NULL,
    expires_at TIMESTAMPTZ NOT NULL,
    decided_at TIMESTAMPTZ,
    created_at TIMESTAMPTZ NOT NULL
);

CREATE INDEX idx_tool_approvals_task ON tool_approvals (task_id, created_at ASC);
