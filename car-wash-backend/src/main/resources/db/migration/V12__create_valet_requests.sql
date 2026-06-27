-- ==========================================
-- V12 — Valet requests table
--
-- Valet is a separate domain from standard
-- bookings (CLAUDE.md hard constraint).
-- The customer supplies their GPS coordinates
-- (from browser navigator.geolocation); the
-- backend validates them against the shop's
-- Haversine radius before accepting.
-- ==========================================

CREATE TABLE valet_requests (
    id              UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    customer_id     UUID NOT NULL,
    location_id     UUID NOT NULL,

    -- Customer pick-up coordinates (supplied by browser geolocation)
    customer_lat    DOUBLE PRECISION NOT NULL,
    customer_lng    DOUBLE PRECISION NOT NULL,
    customer_address VARCHAR(255),              -- optional human-readable label

    -- Distance computed at request time (stored for audit / analytics)
    distance_km     DOUBLE PRECISION NOT NULL,

    -- Configurable per-branch radius used at the time of the request
    radius_km       DOUBLE PRECISION NOT NULL,

    -- Requested pick-up time
    pickup_time     TIMESTAMP NOT NULL,

    vehicle_class   VARCHAR(20) NOT NULL,
    vehicle_model   VARCHAR(100) NOT NULL,

    status          VARCHAR(20) NOT NULL DEFAULT 'PENDING',
    -- PENDING → ACCEPTED → IN_PROGRESS → COMPLETED
    -- PENDING → REJECTED  (outside radius or no capacity)
    -- ACCEPTED → CANCELLED

    notes           TEXT,

    created_at      TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at      TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,

    FOREIGN KEY (customer_id) REFERENCES users(id) ON DELETE RESTRICT,
    FOREIGN KEY (location_id) REFERENCES locations(id) ON DELETE RESTRICT
);

CREATE INDEX idx_valet_customer    ON valet_requests(customer_id);
CREATE INDEX idx_valet_location    ON valet_requests(location_id);
CREATE INDEX idx_valet_status_time ON valet_requests(status, pickup_time);
