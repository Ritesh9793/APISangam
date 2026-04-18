CREATE TABLE user_account (
    id UUID PRIMARY KEY,
    email VARCHAR(255) NOT NULL UNIQUE,
    password_hash VARCHAR(255) NOT NULL,
    full_name VARCHAR(255) NOT NULL,
    company_name VARCHAR(255),
    role VARCHAR(32) NOT NULL,
    mfa_enabled BOOLEAN NOT NULL DEFAULT FALSE,
    mfa_secret VARCHAR(255),
    mfa_recovery_codes_csv VARCHAR(1000),
    enabled BOOLEAN NOT NULL DEFAULT TRUE,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL,
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL
);

CREATE TABLE api_product (
    id UUID PRIMARY KEY,
    provider_id UUID NOT NULL,
    name VARCHAR(255) NOT NULL,
    slug VARCHAR(255) NOT NULL UNIQUE,
    description TEXT NOT NULL,
    category VARCHAR(120) NOT NULL,
    documentation_url VARCHAR(500),
    open_api_spec_url VARCHAR(500),
    base_price NUMERIC(19,2) NOT NULL,
    requests_per_minute INTEGER NOT NULL,
    active BOOLEAN NOT NULL DEFAULT TRUE,
    region VARCHAR(120),
    created_at TIMESTAMP WITH TIME ZONE NOT NULL,
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL,
    CONSTRAINT fk_api_product_provider FOREIGN KEY (provider_id) REFERENCES user_account(id)
);

CREATE TABLE subscription (
    id UUID PRIMARY KEY,
    consumer_id UUID NOT NULL,
    api_product_id UUID NOT NULL,
    plan_name VARCHAR(120) NOT NULL,
    status VARCHAR(32) NOT NULL,
    monthly_price NUMERIC(19,2) NOT NULL,
    monthly_request_limit INTEGER NOT NULL,
    started_at TIMESTAMP WITH TIME ZONE NOT NULL,
    cancelled_at TIMESTAMP WITH TIME ZONE,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL,
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL,
    CONSTRAINT fk_subscription_consumer FOREIGN KEY (consumer_id) REFERENCES user_account(id),
    CONSTRAINT fk_subscription_api_product FOREIGN KEY (api_product_id) REFERENCES api_product(id),
    CONSTRAINT uq_subscription_consumer_api UNIQUE (consumer_id, api_product_id)
);

CREATE TABLE invoice (
    id UUID PRIMARY KEY,
    subscription_id UUID NOT NULL,
    invoice_number VARCHAR(80) NOT NULL UNIQUE,
    status VARCHAR(32) NOT NULL,
    billing_period_start DATE NOT NULL,
    billing_period_end DATE NOT NULL,
    base_amount NUMERIC(19,2) NOT NULL,
    tax_amount NUMERIC(19,2) NOT NULL,
    total_amount NUMERIC(19,2) NOT NULL,
    due_at TIMESTAMP WITH TIME ZONE NOT NULL,
    paid_at TIMESTAMP WITH TIME ZONE,
    payment_provider VARCHAR(80),
    payment_reference VARCHAR(120),
    created_at TIMESTAMP WITH TIME ZONE NOT NULL,
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL,
    CONSTRAINT fk_invoice_subscription FOREIGN KEY (subscription_id) REFERENCES subscription(id)
);

CREATE TABLE usage_record (
    id UUID PRIMARY KEY,
    subscription_id UUID NOT NULL,
    usage_date DATE NOT NULL,
    call_count BIGINT NOT NULL,
    total_cost NUMERIC(19,2) NOT NULL,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL,
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL,
    CONSTRAINT fk_usage_record_subscription FOREIGN KEY (subscription_id) REFERENCES subscription(id) ON DELETE CASCADE,
    CONSTRAINT uq_usage_record_subscription_day UNIQUE (subscription_id, usage_date)
);

CREATE INDEX idx_api_product_category ON api_product(category);
CREATE INDEX idx_subscription_consumer ON subscription(consumer_id);
CREATE INDEX idx_invoice_subscription ON invoice(subscription_id);
CREATE INDEX idx_usage_record_subscription ON usage_record(subscription_id);
