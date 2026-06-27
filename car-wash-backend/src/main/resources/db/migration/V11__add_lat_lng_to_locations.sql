-- ==========================================
-- V11 — Add latitude / longitude to locations
--
-- These columns support the valet geofencing
-- check using the Haversine formula.
-- Both nullable initially to avoid breaking
-- existing seeded rows; set NOT NULL after
-- backfill via owner settings.
-- ==========================================

ALTER TABLE locations
    ADD COLUMN latitude  DOUBLE PRECISION,
    ADD COLUMN longitude DOUBLE PRECISION;
