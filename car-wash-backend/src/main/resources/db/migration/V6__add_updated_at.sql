-- V6__add_updated_at.sql
ALTER TABLE bookings      ADD COLUMN updated_at TIMESTAMP;
ALTER TABLE payments      ADD COLUMN updated_at TIMESTAMP;
ALTER TABLE users         ADD COLUMN updated_at TIMESTAMP;
ALTER TABLE services      ADD COLUMN updated_at TIMESTAMP;
ALTER TABLE locations     ADD COLUMN updated_at TIMESTAMP;
ALTER TABLE slot_capacities ADD COLUMN updated_at TIMESTAMP;