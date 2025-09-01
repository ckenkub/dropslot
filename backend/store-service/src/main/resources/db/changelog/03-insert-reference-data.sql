--liquibase formatted sql
--changeset store:03
-- Insert idempotent mock data for stores and branches
INSERT INTO stores (id, name, slug, tenant_key, logo_url, created_by)
VALUES (uuid_generate_v4(), 'Demo Store', 'demo-store', 'demo', NULL, NULL)
ON CONFLICT (slug) DO NOTHING;

INSERT INTO branches (id, store_id, name, address, lat, lng, phone, opening_hours)
SELECT uuid_generate_v4(), s.id, 'Demo Branch', '123 Demo St', 1.23456, 2.34567, '+1000000000', '{}'::json
FROM stores s WHERE s.slug = 'demo-store'
ON CONFLICT DO NOTHING;

--rollback DELETE FROM branches WHERE name = 'Demo Branch'; DELETE FROM stores WHERE slug = 'demo-store';
