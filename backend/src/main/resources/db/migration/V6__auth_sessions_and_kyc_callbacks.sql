CREATE TABLE refresh_token_session (
    id UUID PRIMARY KEY,
    user_id UUID NOT NULL,
    token_hash VARCHAR(128) NOT NULL UNIQUE,
    expires_at TIMESTAMP WITH TIME ZONE NOT NULL,
    revoked_at TIMESTAMP WITH TIME ZONE,
    last_used_at TIMESTAMP WITH TIME ZONE,
    ip_address VARCHAR(120),
    user_agent VARCHAR(500),
    replaced_by_token_hash VARCHAR(128),
    created_at TIMESTAMP WITH TIME ZONE NOT NULL,
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL,
    CONSTRAINT fk_refresh_token_session_user FOREIGN KEY (user_id) REFERENCES user_account(id) ON DELETE CASCADE
);

CREATE INDEX idx_refresh_token_session_user ON refresh_token_session(user_id);
CREATE INDEX idx_refresh_token_session_expires_at ON refresh_token_session(expires_at);
