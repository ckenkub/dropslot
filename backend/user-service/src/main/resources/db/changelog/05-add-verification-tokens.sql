--liquibase formatted sql
--changeset ckenkub:05
CREATE TABLE IF NOT EXISTS verification_tokens (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    email TEXT NOT NULL,
    token TEXT NOT NULL,
    type TEXT NOT NULL,
    created_at TIMESTAMP NOT NULL DEFAULT now(),
    expires_at TIMESTAMP NULL
);

CREATE INDEX IF NOT EXISTS idx_verification_tokens_email_type ON verification_tokens(email, type);

--rollback DROP TABLE IF EXISTS verification_tokens;
