-- ==========================================
-- V14 — Restore booked_count CHECK constraint
--
-- The original CHECK (booked_count >= 0) from V1 was lost when V5 dropped
-- the booked_count column and V10 re-added it without the constraint.
-- The application-level guard in decrementBookedCount (booked_count > 0)
-- prevents going negative, but this adds DB-level enforcement as a safety net.
-- ==========================================

ALTER TABLE slot_capacities
    ADD CONSTRAINT chk_booked_count CHECK (booked_count >= 0);
