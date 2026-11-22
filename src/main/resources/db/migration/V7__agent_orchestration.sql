CREATE TABLE agent_runs (
    id UUID PRIMARY KEY,
    project_id UUID NOT NULL REFERENCES projects(id) ON DELETE CASCADE,
    owner_user_id UUID NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    goal VARCHAR(2000) NOT NULL,
    model_id VARCHAR(120) NOT NULL,
    plan_summary TEXT NOT NULL,
    status VARCHAR(32) NOT NULL,
    workspace_file_count INTEGER NOT NULL DEFAULT 0,
    rag_match_count INTEGER NOT NULL DEFAULT 0,
    created_at TIMESTAMPTZ NOT NULL,
    updated_at TIMESTAMPTZ NOT NULL
);

CREATE INDEX idx_agent_runs_project_owner ON agent_runs (project_id, owner_user_id, created_at DESC);

CREATE TABLE agent_tool_proposals (
    id UUID PRIMARY KEY,
    run_id UUID NOT NULL REFERENCES agent_runs(id) ON DELETE CASCADE,
    project_id UUID NOT NULL REFERENCES projects(id) ON DELETE CASCADE,
    owner_user_id UUID NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    step_index INTEGER NOT NULL,
    tool_name VARCHAR(40) NOT NULL,
    target_path VARCHAR(260),
    command_text VARCHAR(1000),
    rationale VARCHAR(500) NOT NULL,
    policy_decision VARCHAR(32) NOT NULL,
    policy_reason VARCHAR(240) NOT NULL,
    approval_status VARCHAR(32) NOT NULL,
    decided_at TIMESTAMPTZ,
    created_at TIMESTAMPTZ NOT NULL,
    CONSTRAINT uq_agent_proposals_run_step UNIQUE (run_id, step_index)
);

CREATE INDEX idx_agent_proposals_run ON agent_tool_proposals (run_id, step_index ASC);

ALTER TABLE usage_records DROP CONSTRAINT chk_usage_records_source;
ALTER TABLE usage_records ADD CONSTRAINT chk_usage_records_source
    CHECK (source IN ('CHAT_SYNC', 'CHAT_STREAM', 'AGENT_PLAN'));
