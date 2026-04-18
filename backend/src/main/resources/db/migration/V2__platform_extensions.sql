CREATE TABLE api_key (
    id UUID PRIMARY KEY,
    subscription_id UUID NOT NULL,
    owner_id UUID NOT NULL,
    api_product_id UUID NOT NULL,
    key_prefix VARCHAR(32) NOT NULL,
    key_hash VARCHAR(255) NOT NULL,
    label VARCHAR(255) NOT NULL,
    scopes_csv VARCHAR(500) NOT NULL,
    expires_at TIMESTAMP WITH TIME ZONE,
    last_used_at TIMESTAMP WITH TIME ZONE,
    active BOOLEAN NOT NULL DEFAULT TRUE,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL,
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL,
    CONSTRAINT fk_api_key_subscription FOREIGN KEY (subscription_id) REFERENCES subscription(id) ON DELETE CASCADE,
    CONSTRAINT fk_api_key_owner FOREIGN KEY (owner_id) REFERENCES user_account(id) ON DELETE CASCADE,
    CONSTRAINT fk_api_key_api_product FOREIGN KEY (api_product_id) REFERENCES api_product(id) ON DELETE CASCADE
);

CREATE INDEX idx_api_key_owner ON api_key(owner_id);
CREATE INDEX idx_api_key_subscription ON api_key(subscription_id);
CREATE INDEX idx_api_key_prefix ON api_key(key_prefix);

CREATE TABLE api_call_log (
    id UUID PRIMARY KEY,
    api_key_id UUID NOT NULL,
    subscription_id UUID NOT NULL,
    api_product_id UUID NOT NULL,
    request_path VARCHAR(500) NOT NULL,
    http_method VARCHAR(16) NOT NULL,
    status_code INTEGER NOT NULL,
    latency_ms BIGINT NOT NULL,
    request_cost NUMERIC(19,2) NOT NULL,
    ip_address VARCHAR(120),
    user_agent VARCHAR(500),
    occurred_at TIMESTAMP WITH TIME ZONE NOT NULL,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL,
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL,
    CONSTRAINT fk_api_call_log_api_key FOREIGN KEY (api_key_id) REFERENCES api_key(id) ON DELETE CASCADE,
    CONSTRAINT fk_api_call_log_subscription FOREIGN KEY (subscription_id) REFERENCES subscription(id) ON DELETE CASCADE,
    CONSTRAINT fk_api_call_log_api_product FOREIGN KEY (api_product_id) REFERENCES api_product(id) ON DELETE CASCADE
);

CREATE INDEX idx_api_call_log_subscription ON api_call_log(subscription_id);
CREATE INDEX idx_api_call_log_api_product ON api_call_log(api_product_id);
CREATE INDEX idx_api_call_log_occurred_at ON api_call_log(occurred_at);
