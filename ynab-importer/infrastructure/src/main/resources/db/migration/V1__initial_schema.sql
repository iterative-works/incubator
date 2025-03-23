-- Initial consolidated schema for the database
-- Creates the source_account, transaction, and transaction_processing_state tables

-- Transaction status enum
CREATE TYPE transaction_status AS ENUM ('Imported', 'Categorized', 'Submitted');

-- Source accounts table
CREATE TABLE source_account (
    id BIGINT PRIMARY KEY,
    account_id VARCHAR(255) NOT NULL,
    bank_id VARCHAR(255) NOT NULL,
    name VARCHAR(255) NOT NULL DEFAULT 'Unnamed Account',
    currency VARCHAR(3) NOT NULL DEFAULT 'CZK',
    ynab_account_id VARCHAR(255),
    active BOOLEAN NOT NULL DEFAULT true,
    last_sync_time TIMESTAMP WITH TIME ZONE,
    UNIQUE (account_id, bank_id)
);

-- Transaction table (immutable events)
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

-- Transaction processing state table
CREATE TABLE transaction_processing_state (
    -- Reference to transaction
    source_account_id BIGINT NOT NULL,
    transaction_id VARCHAR(255) NOT NULL,
    
    -- Processing state
    -- status transaction_status NOT NULL,
    -- TODO: use transaction_status after next Magnum release
    status VARCHAR(255) NOT NULL,
    
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