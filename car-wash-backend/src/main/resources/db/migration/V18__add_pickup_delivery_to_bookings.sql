-- Pickup / delivery add-ons on bookings (flat fee each, configurable via shop_settings).
-- ValetRequest remains a separate standalone entity; these columns are booking add-ons.
ALTER TABLE bookings
    ADD COLUMN pickup_requested   BOOLEAN NOT NULL DEFAULT FALSE,
    ADD COLUMN delivery_requested BOOLEAN NOT NULL DEFAULT FALSE,
    ADD COLUMN pickup_address     VARCHAR(255),
    ADD COLUMN pickup_lat         DOUBLE PRECISION,
    ADD COLUMN pickup_lng         DOUBLE PRECISION,
    ADD COLUMN pickup_notes       TEXT;

INSERT INTO shop_settings (setting_key, setting_value) VALUES
    ('pickup_fee',   '5.00'),
    ('delivery_fee', '5.00');
