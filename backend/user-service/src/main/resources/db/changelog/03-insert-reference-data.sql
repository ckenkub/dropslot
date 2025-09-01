--liquibase formatted sql
--changeset ckenkub:03
INSERT INTO roles (id, code, name) VALUES (uuid_generate_v4(), 'ADMIN', 'Administrator') ON CONFLICT DO NOTHING;
INSERT INTO roles (id, code, name) VALUES (uuid_generate_v4(), 'CUSTOMER', 'Customer') ON CONFLICT DO NOTHING;
INSERT INTO roles (id, code, name) VALUES (uuid_generate_v4(), 'MANAGER', 'Store Manager') ON CONFLICT DO NOTHING;

--rollback DELETE FROM roles WHERE code IN ('ADMIN','CUSTOMER','MANAGER');
