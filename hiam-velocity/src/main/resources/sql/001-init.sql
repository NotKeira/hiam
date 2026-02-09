-- Helix IAM Database Schema (v2)
-- PostgreSQL 12+

-- Ensure pgcrypto is available for gen_random_uuid()
CREATE EXTENSION IF NOT EXISTS pgcrypto;

-- Base accounts table
CREATE TABLE IF NOT EXISTS accounts (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    username VARCHAR(16) NOT NULL UNIQUE,
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    last_ip INET NULL,
    last_login_at TIMESTAMPTZ NULL
);

-- Drop legacy mode column if present
ALTER TABLE accounts DROP COLUMN IF EXISTS mode;

-- Credentials table for multiple auth methods (password + premium UUID)
CREATE TABLE IF NOT EXISTS account_credentials (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    account_id UUID NOT NULL REFERENCES accounts(id) ON DELETE CASCADE,
    type VARCHAR(20) NOT NULL,
    secret VARCHAR(255) NULL,
    premium_uuid UUID NULL,
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT credential_type_check CHECK (type IN ('PASSWORD', 'PREMIUM_UUID')),
    CONSTRAINT credential_unique UNIQUE (account_id, type)
);

-- Sessions table
CREATE TABLE IF NOT EXISTS sessions (
    session_id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    account_id UUID NOT NULL REFERENCES accounts(id) ON DELETE CASCADE,
    issued_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    expires_at TIMESTAMPTZ NOT NULL,
    ip INET NOT NULL,
    user_agent TEXT NULL,
    auth_type VARCHAR(20) NULL,
    is_premium BOOLEAN NOT NULL DEFAULT FALSE
);

-- Backfill/upgrade older session tables safely
ALTER TABLE sessions ADD COLUMN IF NOT EXISTS auth_type VARCHAR(20) NULL;
ALTER TABLE sessions ADD COLUMN IF NOT EXISTS is_premium BOOLEAN NOT NULL DEFAULT FALSE;

-- Login attempts (rate limiting)
CREATE TABLE IF NOT EXISTS login_attempts (
    id SERIAL PRIMARY KEY,
    username VARCHAR(16) NOT NULL,
    ip INET NOT NULL,
    attempt_time TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    success BOOLEAN NOT NULL DEFAULT FALSE
);

-- Indexes
CREATE INDEX IF NOT EXISTS idx_accounts_username ON accounts(username);
CREATE INDEX IF NOT EXISTS idx_credentials_account_type ON account_credentials(account_id, type);
CREATE INDEX IF NOT EXISTS idx_credentials_premium_uuid ON account_credentials(premium_uuid) WHERE premium_uuid IS NOT NULL;
CREATE INDEX IF NOT EXISTS idx_sessions_account_id ON sessions(account_id);
CREATE INDEX IF NOT EXISTS idx_sessions_expires_at ON sessions(expires_at);
CREATE INDEX IF NOT EXISTS idx_login_attempts_username_time ON login_attempts(username, attempt_time);
CREATE INDEX IF NOT EXISTS idx_login_attempts_ip_time ON login_attempts(ip, attempt_time);

-- Updated_at trigger function
CREATE OR REPLACE FUNCTION update_updated_at_column()
RETURNS TRIGGER AS $$
BEGIN
    NEW.updated_at = CURRENT_TIMESTAMP;
    RETURN NEW;
END;
$$ LANGUAGE plpgsql;

-- Triggers for updated_at
DO $$
BEGIN
    IF NOT EXISTS (
        SELECT 1 FROM pg_trigger
        WHERE tgname = 'update_accounts_updated_at'
          AND tgrelid = 'accounts'::regclass
    ) THEN
        CREATE TRIGGER update_accounts_updated_at
            BEFORE UPDATE ON accounts
            FOR EACH ROW
            EXECUTE FUNCTION update_updated_at_column();
    END IF;
END $$;

DO $$
BEGIN
    IF NOT EXISTS (
        SELECT 1 FROM pg_trigger
        WHERE tgname = 'update_credentials_updated_at'
          AND tgrelid = 'account_credentials'::regclass
    ) THEN
        CREATE TRIGGER update_credentials_updated_at
            BEFORE UPDATE ON account_credentials
            FOR EACH ROW
            EXECUTE FUNCTION update_updated_at_column();
    END IF;
END $$;

-- Cleanup function for old login attempts
CREATE OR REPLACE FUNCTION cleanup_old_login_attempts()
RETURNS void AS $$
BEGIN
    DELETE FROM login_attempts
    WHERE attempt_time < CURRENT_TIMESTAMP - INTERVAL '1 hour';
END;
$$ LANGUAGE plpgsql;
