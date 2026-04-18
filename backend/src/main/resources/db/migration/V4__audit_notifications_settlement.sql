CREATE TABLE audit_log (
    id UUID PRIMARY KEY,
    actor_id UUID,
    actor_email VARCHAR(255),
    actor_role VARCHAR(64),
    event_type VARCHAR(120) NOT NULL,
    target_type VARCHAR(120),
    target_id VARCHAR(120),
    status VARCHAR(32) NOT NULL,
    message TEXT,
    metadata TEXT,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL,
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL
);

CREATE INDEX idx_audit_log_actor ON audit_log(actor_id);
CREATE INDEX idx_audit_log_event_type ON audit_log(event_type);

CREATE TABLE notification_event (
    id UUID PRIMARY KEY,
    user_id UUID,
    channel VARCHAR(32) NOT NULL,
    recipient VARCHAR(255) NOT NULL,
    subject VARCHAR(255),
    message TEXT NOT NULL,
    event_type VARCHAR(120) NOT NULL,
    status VARCHAR(32) NOT NULL,
    provider VARCHAR(120) NOT NULL DEFAULT 'SIMULATED',
    delivered_at TIMESTAMP WITH TIME ZONE,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL,
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL
);

CREATE INDEX idx_notification_event_user ON notification_event(user_id);
CREATE INDEX idx_notification_event_channel ON notification_event(channel);

CREATE TABLE settlement_batch (
    id UUID PRIMARY KEY,
    provider_id UUID NOT NULL,
    period_start DATE NOT NULL,
    period_end DATE NOT NULL,
    invoice_count INTEGER NOT NULL,
    gross_revenue NUMERIC(19,2) NOT NULL,
    platform_fee_rate NUMERIC(8,4) NOT NULL,
    platform_fee NUMERIC(19,2) NOT NULL,
    tax_amount NUMERIC(19,2) NOT NULL,
    net_payout NUMERIC(19,2) NOT NULL,
    status VARCHAR(32) NOT NULL,
    generated_at TIMESTAMP WITH TIME ZONE NOT NULL,
    paid_at TIMESTAMP WITH TIME ZONE,
    payout_reference VARCHAR(120),
    created_at TIMESTAMP WITH TIME ZONE NOT NULL,
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL,
    CONSTRAINT fk_settlement_batch_provider FOREIGN KEY (provider_id) REFERENCES user_account(id)
);

CREATE UNIQUE INDEX uq_settlement_batch_provider_period ON settlement_batch(provider_id, period_start, period_end);
CREATE INDEX idx_settlement_batch_status ON settlement_batch(status);

CREATE TABLE payout_record (
    id UUID PRIMARY KEY,
    settlement_batch_id UUID NOT NULL,
    provider_id UUID NOT NULL,
    amount NUMERIC(19,2) NOT NULL,
    payout_mode VARCHAR(64) NOT NULL,
    status VARCHAR(32) NOT NULL,
    provider_reference VARCHAR(120),
    processed_at TIMESTAMP WITH TIME ZONE,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL,
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL,
    CONSTRAINT fk_payout_record_batch FOREIGN KEY (settlement_batch_id) REFERENCES settlement_batch(id) ON DELETE CASCADE,
    CONSTRAINT fk_payout_record_provider FOREIGN KEY (provider_id) REFERENCES user_account(id)
);

CREATE INDEX idx_payout_record_provider ON payout_record(provider_id);
CREATE INDEX idx_payout_record_batch ON payout_record(settlement_batch_id);
