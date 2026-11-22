ALTER TABLE sessions
    ADD COLUMN client_id VARCHAR(64) NOT NULL DEFAULT 'api',
    ADD COLUMN auth_method VARCHAR(32) NOT NULL DEFAULT 'PASSWORD',
    ADD COLUMN family_id UUID,
    ADD COLUMN refresh_token_hash VARCHAR(64),
    ADD COLUMN refresh_expires_at TIMESTAMPTZ,
    ADD COLUMN rotated_at TIMESTAMPTZ;

UPDATE sessions SET family_id = id WHERE family_id IS NULL;

ALTER TABLE sessions
    ALTER COLUMN family_id SET NOT NULL;

CREATE UNIQUE INDEX uk_sessions_refresh_token_hash
    ON sessions (refresh_token_hash)
    WHERE refresh_token_hash IS NOT NULL;

CREATE INDEX idx_sessions_family_id ON sessions (family_id);

CREATE TABLE authorization_codes (
    id UUID PRIMARY KEY,
    user_id UUID NOT NULL,
    client_id VARCHAR(64) NOT NULL,
    redirect_uri VARCHAR(512) NOT NULL,
    code_hash VARCHAR(64) NOT NULL,
    code_challenge VARCHAR(128) NOT NULL,
    code_challenge_method VARCHAR(16) NOT NULL,
    expires_at TIMESTAMPTZ NOT NULL,
    used_at TIMESTAMPTZ,
    created_at TIMESTAMPTZ NOT NULL,
    CONSTRAINT fk_auth_codes_user FOREIGN KEY (user_id) REFERENCES users (id) ON DELETE CASCADE,
    CONSTRAINT uk_auth_codes_hash UNIQUE (code_hash),
    CONSTRAINT chk_auth_codes_method CHECK (code_challenge_method = 'S256')
);

CREATE INDEX idx_auth_codes_user_id ON authorization_codes (user_id);
