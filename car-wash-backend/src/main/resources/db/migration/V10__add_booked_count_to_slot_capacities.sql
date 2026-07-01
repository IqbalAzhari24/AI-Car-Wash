ALTER TABLE slot_capacities
    ADD COLUMN IF NOT EXISTS booked_count INTEGER NOT NULL DEFAULT 0;
    