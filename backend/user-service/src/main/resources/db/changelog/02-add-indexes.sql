--liquibase formatted sql
--changeset ckenkub:02
CREATE INDEX IF NOT EXISTS idx_users_email ON users(email);
CREATE INDEX IF NOT EXISTS idx_users_status ON users(status);

--rollback DROP INDEX IF EXISTS idx_users_status; DROP INDEX IF EXISTS idx_users_email;
