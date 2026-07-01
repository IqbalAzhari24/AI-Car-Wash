-- V5__fix_slot_concurrency.sql
-- ponytail: derived count, add booked_count materialized column if query is slow
ALTER TABLE slot_capacities DROP COLUMN booked_count;
