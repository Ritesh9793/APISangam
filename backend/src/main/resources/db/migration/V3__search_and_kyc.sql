CREATE TABLE kyc_application (
    id UUID PRIMARY KEY,
    user_id UUID NOT NULL UNIQUE,
    legal_business_name VARCHAR(255) NOT NULL,
    contact_name VARCHAR(255),
    email VARCHAR(255) NOT NULL,
    phone_number VARCHAR(32),
    business_type VARCHAR(120) NOT NULL,
    pan_number VARCHAR(10),
    gstin VARCHAR(15),
    bank_account_masked VARCHAR(64),
    bank_ifsc VARCHAR(20),
    registered_address TEXT,
    status VARCHAR(32) NOT NULL,
    pan_verified BOOLEAN NOT NULL DEFAULT FALSE,
    gstin_verified BOOLEAN NOT NULL DEFAULT FALSE,
    bank_verified BOOLEAN NOT NULL DEFAULT FALSE,
    rejection_reason TEXT,
    reviewer_id UUID,
    submitted_at TIMESTAMP WITH TIME ZONE,
    reviewed_at TIMESTAMP WITH TIME ZONE,
    supporting_documents_csv VARCHAR(1000),
    created_at TIMESTAMP WITH TIME ZONE NOT NULL,
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL,
    CONSTRAINT fk_kyc_application_user FOREIGN KEY (user_id) REFERENCES user_account(id) ON DELETE CASCADE,
    CONSTRAINT fk_kyc_application_reviewer FOREIGN KEY (reviewer_id) REFERENCES user_account(id) ON DELETE SET NULL
);

CREATE INDEX idx_kyc_application_status ON kyc_application(status);
CREATE INDEX idx_kyc_application_user ON kyc_application(user_id);
