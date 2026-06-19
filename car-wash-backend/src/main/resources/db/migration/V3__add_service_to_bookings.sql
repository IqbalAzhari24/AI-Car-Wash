-- ==========================================
-- Link bookings to the services catalogue
--
-- Design notes:
--  * services previously had no relationship to bookings, so there was no
--    way to tell which wash type a booking was for.
--  * service_id is nullable: existing bookings predate this column and have
--    no value to backfill, and is_override bookings (manual/clerk entries)
--    may not map to a catalogue service.
--  * ON DELETE RESTRICT mirrors the locations FK on bookings — a service
--    that has been booked cannot be hard-deleted (use services.is_active
--    to retire it instead).
-- ==========================================

ALTER TABLE bookings ADD COLUMN service_id UUID;

ALTER TABLE bookings
    ADD CONSTRAINT fk_bookings_service
    FOREIGN KEY (service_id) REFERENCES services(id) ON DELETE RESTRICT;

-- Reporting: "most booked service" / per-service breakdowns
CREATE INDEX idx_bookings_service ON bookings(service_id);
