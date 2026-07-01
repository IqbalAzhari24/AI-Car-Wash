-- V8__seed_catalog.sql
-- Static catalog data: default branch and wash services.
-- Slot inventory is generated on boot by CatalogSeeder (date-relative).

INSERT INTO locations (id, name, address, is_active)
VALUES (uuid_generate_v4(), 'Jalan Utama, Kuala Lumpur', TRUE)
ON CONFLICT DO NOTHING;

INSERT INTO services (id, name, description, price, duration_minutes, vehicle_size_multiplier, is_active)
VALUES
    (uuid_generate_v4(), 'Standard Wash',  'Exterior wash and dry',                    25.00,  30, 1.00, TRUE),
    (uuid_generate_v4(), 'Premium Wash',   'Exterior + interior vacuum and wipe-down', 45.00,  45, 1.00, TRUE),
    (uuid_generate_v4(), 'Full Detailing', 'Deep clean, polish and wax',              120.00,  90, 1.00, TRUE)
ON CONFLICT DO NOTHING;
