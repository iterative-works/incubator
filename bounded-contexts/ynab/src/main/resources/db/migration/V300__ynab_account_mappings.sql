-- Add ynab_account_mappings table
CREATE TABLE IF NOT EXISTS ynab_account_mappings (
    source_account_id BIGINT PRIMARY KEY,
    ynab_account_id VARCHAR(255) NOT NULL,
    active BOOLEAN NOT NULL DEFAULT true,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW()
);

-- Create index on ynab_account_id
CREATE INDEX IF NOT EXISTS ynab_account_mappings_ynab_account_id_idx ON ynab_account_mappings (ynab_account_id);

-- Migrate existing data from source_account table
INSERT INTO ynab_account_mappings (source_account_id, ynab_account_id, active)
SELECT id, ynab_account_id, active
FROM source_account
WHERE ynab_account_id IS NOT NULL;