-- ==========================================
-- V13 — Backfill coordinates for the seeded branch
--
-- The valet geofencing check (Haversine) rejects requests with a 500 when the
-- branch has no GPS coordinates. The default branch from V8 was inserted before
-- the lat/lng columns existed (V11), so backfill it here. Kuala Lumpur city centre.
-- Only touches rows still missing coordinates.
-- ==========================================

UPDATE locations
SET latitude  = 3.1390,
    longitude = 101.6869
WHERE name = 'Timah Wash - Main'
  AND latitude IS NULL;
