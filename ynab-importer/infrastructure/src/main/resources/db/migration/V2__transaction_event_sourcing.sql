-- V2: Update schema to support event-based transaction model
-- Separate transaction data from processing state

-- Modify source_account table to add new fields
ALTER TABLE source_account 
ADD COLUMN name VARCHAR(255) NOT NULL DEFAULT 'Unnamed Account',
ADD COLUMN currency VARCHAR(3) NOT NULL DEFAULT 'CZK',
ADD COLUMN ynab_account_id VARCHAR(255),
ADD COLUMN active BOOLEAN NOT NULL DEFAULT true,
ADD COLUMN last_sync_time TIMESTAMP WITH TIME ZONE;

-- Create new transaction table structure (dropping constraints first)
ALTER TABLE transaction DROP CONSTRAINT IF EXISTS transaction_pkey;

-- Rename the old transaction table to transaction_old
ALTER TABLE transaction RENAME TO transaction_old;

-- Create new transaction table (immutable events)
CREATE TABLE transaction (
    -- Primary key and identifiers
    source_account_id BIGINT NOT NULL,
    transaction_id VARCHAR(255) NOT NULL,
    
    -- Transaction details
    date DATE NOT NULL,
    amount DECIMAL(19, 4) NOT NULL,
    currency VARCHAR(10) NOT NULL,
    
    -- Counterparty information
    counter_account VARCHAR(255),
    counter_bank_code VARCHAR(255),
    counter_bank_name VARCHAR(255),
    
    -- Additional transaction details
    variable_symbol VARCHAR(255),
    constant_symbol VARCHAR(255),
    specific_symbol VARCHAR(255),
    user_identification VARCHAR(255),
    message TEXT,
    transaction_type VARCHAR(255) NOT NULL,
    comment TEXT,
    
    -- Metadata
    imported_at TIMESTAMP WITH TIME ZONE NOT NULL,
    
    -- Composite primary key
    PRIMARY KEY (source_account_id, transaction_id),
    
    -- Foreign key to source_account
    CONSTRAINT fk_transaction_source_account
        FOREIGN KEY (source_account_id)
        REFERENCES source_account(id)
);

-- Create transaction_processing_state table
CREATE TABLE transaction_processing_state (
    -- Reference to transaction
    source_account_id BIGINT NOT NULL,
    transaction_id VARCHAR(255) NOT NULL,
    
    -- Processing state
    status transaction_status NOT NULL,
    
    -- AI computed/processed fields for YNAB
    suggested_payee_name VARCHAR(255),
    suggested_category VARCHAR(255),
    suggested_memo TEXT,
    
    -- User overrides
    override_payee_name VARCHAR(255),
    override_category VARCHAR(255),
    override_memo TEXT,
    
    -- YNAB integration fields
    ynab_transaction_id VARCHAR(255),
    ynab_account_id VARCHAR(255),
    
    -- Processing timestamps
    processed_at TIMESTAMP WITH TIME ZONE,
    submitted_at TIMESTAMP WITH TIME ZONE,
    
    -- Primary key (same as transaction)
    PRIMARY KEY (source_account_id, transaction_id),
    
    -- Foreign key to transaction
    CONSTRAINT fk_processing_state_transaction
        FOREIGN KEY (source_account_id, transaction_id)
        REFERENCES transaction(source_account_id, transaction_id)
);

-- Indexes for performance
CREATE INDEX idx_transaction_date ON transaction(date);
CREATE INDEX idx_transaction_source_account ON transaction(source_account_id);
CREATE INDEX idx_transaction_imported_at ON transaction(imported_at);
CREATE INDEX idx_processing_state_status ON transaction_processing_state(status);
CREATE INDEX idx_processing_state_source_account ON transaction_processing_state(source_account_id);

-- Migration function to transfer data from the old table to new tables
-- This will be executed manually after deployment with a script
-- CREATE OR REPLACE FUNCTION migrate_transaction_data() RETURNS void AS $$
-- BEGIN
--    -- Insert code to migrate data here
-- END;
-- $$ LANGUAGE plpgsql;