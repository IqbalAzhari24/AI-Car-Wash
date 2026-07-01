-- V7__add_soft_delete.sql
ALTER TABLE users    ADD COLUMN deleted_at TIMESTAMP;
ALTER TABLE bookings ADD COLUMN deleted_at TIMESTAMP;
