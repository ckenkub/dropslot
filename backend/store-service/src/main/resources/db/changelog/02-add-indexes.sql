--liquibase formatted sql
--changeset ckenkub:02
CREATE INDEX IF NOT EXISTS idx_stores_slug ON stores(slug);
CREATE INDEX IF NOT EXISTS idx_stores_tenant_key ON stores(tenant_key);
CREATE INDEX IF NOT EXISTS idx_branches_store_id ON branches(store_id);

--rollback DROP INDEX IF EXISTS idx_branches_store_id; DROP INDEX IF EXISTS idx_stores_tenant_key; DROP INDEX IF EXISTS idx_stores_slug;
