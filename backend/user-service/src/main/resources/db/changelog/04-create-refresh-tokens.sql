--liquibase formatted sql
--changeset dropslot:create-refresh-tokens
CREATE TABLE IF NOT EXISTS refresh_tokens (
    id UUID PRIMARY KEY,
    user_id UUID NOT NULL,
    jti VARCHAR(100) NOT NULL UNIQUE,
    issued_at TIMESTAMP WITHOUT TIME ZONE NOT NULL,
    expires_at TIMESTAMP WITHOUT TIME ZONE NOT NULL,
    revoked BOOLEAN NOT NULL DEFAULT FALSE,
    replaced_by_jti VARCHAR(100),
    CONSTRAINT fk_refresh_user FOREIGN KEY(user_id) REFERENCES users(id) ON DELETE CASCADE
);

--rollback DROP TABLE refresh_tokens;
