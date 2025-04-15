-- Migration file for Fio integration tables
-- Creates the fio_import_state table for tracking Fio Bank import state

-- Table for tracking Fio import state
CREATE TABLE IF NOT EXISTS fio_import_state (
    -- The source account ID this import state is for
    source_account_id BIGINT PRIMARY KEY,
    
    -- The last transaction ID that was imported
    last_transaction_id BIGINT,
    
    -- The timestamp when the last import occurred
    last_import_timestamp TIMESTAMP WITH TIME ZONE NOT NULL,
    
    -- Foreign key to source_account
    CONSTRAINT fk_fio_import_state_source_account
        FOREIGN KEY (source_account_id)
        REFERENCES source_account(id)
);

-- Index for performance
CREATE INDEX idx_fio_import_state_source_account ON fio_import_state(source_account_id);

-- Comment for documentation
COMMENT ON TABLE fio_import_state IS 'Tracks the state of Fio Bank transaction imports for each source account';
COMMENT ON COLUMN fio_import_state.source_account_id IS 'The source account ID this import state is for';
COMMENT ON COLUMN fio_import_state.last_transaction_id IS 'The last transaction ID that was imported from Fio API';
COMMENT ON COLUMN fio_import_state.last_import_timestamp IS 'The timestamp when the last import occurred';