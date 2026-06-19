-- ============================================================
-- AzureWash — Flyway V4
-- Creates the password_reset_tokens table for the
-- forgot-password flow.
--
-- Security design:
--   token_hash stores the SHA-256 hex digest only.
--   The raw token is sent by email and never persisted.
--   Tokens are single-use (used flag) and time-bounded (expires_at).
-- ============================================================

CREATE TABLE password_reset_tokens (
    id          UUID        NOT NULL DEFAULT gen_random_uuid() PRIMARY KEY,
    user_id     UUID        NOT NULL,
    token_hash  CHAR(64)    NOT NULL,
    expires_at  TIMESTAMP   NOT NULL,
    used        BOOLEAN     NOT NULL DEFAULT FALSE,
    created_at  TIMESTAMP   NOT NULL DEFAULT NOW(),
    updated_at  TIMESTAMP   NOT NULL DEFAULT NOW(),

    CONSTRAINT fk_prt_user
        FOREIGN KEY (user_id) REFERENCES users (id)
        ON DELETE CASCADE,

    CONSTRAINT uq_prt_token_hash
        UNIQUE (token_hash)
);

CREATE INDEX idx_prt_token_hash   ON password_reset_tokens (token_hash);
CREATE INDEX idx_prt_user_id_used ON password_reset_tokens (user_id, used);

COMMENT ON TABLE  password_reset_tokens              IS 'Short-lived single-use tokens for the forgot-password flow';
COMMENT ON COLUMN password_reset_tokens.token_hash   IS 'SHA-256 hex digest of the raw URL-safe base64 token emailed to the user';
COMMENT ON COLUMN password_reset_tokens.expires_at   IS 'UTC timestamp after which the token is invalid';
COMMENT ON COLUMN password_reset_tokens.used         IS 'TRUE once the token has been successfully consumed';
