-- Migration script for Fio account table
-- Creates a table for storing Fio Bank account configuration

CREATE TABLE fio_account (
    id BIGSERIAL PRIMARY KEY,
    source_account_id BIGINT NOT NULL REFERENCES source_account(id),
    token VARCHAR(100) NOT NULL,
    last_sync_time TIMESTAMP,
    last_fetched_id BIGINT,
    UNIQUE (source_account_id)
);

-- Add index for faster lookups by source account ID
CREATE INDEX fio_account_source_account_id_idx ON fio_account(source_account_id);

-- Add comments for documentation
COMMENT ON TABLE fio_account IS 'Stores Fio Bank account configuration and sync state';
COMMENT ON COLUMN fio_account.id IS 'Primary key for Fio accounts';
COMMENT ON COLUMN fio_account.source_account_id IS 'Foreign key to source_account table';
COMMENT ON COLUMN fio_account.token IS 'Fio Bank API token (encrypted)';
COMMENT ON COLUMN fio_account.last_sync_time IS 'Timestamp of last successful transaction import';
COMMENT ON COLUMN fio_account.last_fetched_id IS 'ID of the last transaction fetched from Fio API';