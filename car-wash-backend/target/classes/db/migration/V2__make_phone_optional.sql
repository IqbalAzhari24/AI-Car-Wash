-- phone_number was NOT NULL UNIQUE in V1. Staff provisioning does not require a phone number,
-- so we drop both constraints. Phone uniqueness makes no sense for an optional field.
ALTER TABLE users ALTER COLUMN phone_number DROP NOT NULL;
ALTER TABLE users DROP CONSTRAINT IF EXISTS users_phone_number_key;
