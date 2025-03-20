-- Initial schema for the database
-- Creates the transaction and source_account tables

-- Transaction status enum
CREATE TYPE transaction_status AS ENUM ('IMPORTED', 'CATEGORIZED', 'SUBMITTED');

-- Source accounts table
CREATE TABLE source_account (
    id BIGINT PRIMARY KEY,
    account_id VARCHAR(255) NOT NULL,
    bank_id VARCHAR(255) NOT NULL,
    UNIQUE (account_id, bank_id)
);

-- Transactions table
CREATE TABLE transaction (
    -- Source data from FIO
    source_account VARCHAR(255) NOT NULL,
    source_bank VARCHAR(255) NOT NULL,
    transaction_id VARCHAR(255) NOT NULL,
    date DATE NOT NULL,
    amount DECIMAL(19, 4) NOT NULL,
    currency VARCHAR(10) NOT NULL,
    counter_account VARCHAR(255),
    counter_bank_code VARCHAR(255),
    counter_bank_name VARCHAR(255),
    variable_symbol VARCHAR(255),
    constant_symbol VARCHAR(255),
    specific_symbol VARCHAR(255),
    user_identification VARCHAR(255),
    message TEXT,
    transaction_type VARCHAR(255) NOT NULL,
    comment TEXT,

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

    -- Metadata
    imported_at TIMESTAMP WITH TIME ZONE NOT NULL,
    processed_at TIMESTAMP WITH TIME ZONE,
    submitted_at TIMESTAMP WITH TIME ZONE,

    -- Composite primary key
    PRIMARY KEY (source_account, source_bank, transaction_id)
);

-- Indexes for performance
CREATE INDEX idx_transaction_status ON transaction(status);
CREATE INDEX idx_transaction_date ON transaction(date);
CREATE INDEX idx_transaction_imported_at ON transaction(imported_at);