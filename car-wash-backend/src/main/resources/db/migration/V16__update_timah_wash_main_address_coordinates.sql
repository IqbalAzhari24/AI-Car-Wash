-- ==========================================
-- V16 — Update Timah Wash - Main address and real coordinates
--
-- Replaces V13's Kuala Lumpur placeholder with the branch's actual
-- GPS coordinates (Kuala Terengganu), used by the valet geofencing
-- Haversine check.
-- ==========================================

UPDATE locations
SET address   = 'PT30125 A, Kampung Gong Pa'' Jin, Kampung Wakaf Tengah, Terengganu',
    latitude  = 5.4153,
    longitude = 103.0653
WHERE name = 'Timah Wash - Main';
