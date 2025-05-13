CREATE TABLE fio_accounts (
  id BIGINT PRIMARY KEY,
  source_account_id VARCHAR(50) NOT NULL,
  encrypted_token TEXT NOT NULL,
  last_sync_time TIMESTAMP WITH TIME ZONE,
  last_fetched_id BIGINT,
  created_at TIMESTAMP WITH TIME ZONE NOT NULL,
  updated_at TIMESTAMP WITH TIME ZONE NOT NULL
);

-- Index for finding account by source_account_id
CREATE INDEX idx_fio_accounts_source_account_id
  ON fio_accounts(source_account_id);

-- Index for tracking last sync time
CREATE INDEX idx_fio_accounts_last_sync_time
  ON fio_accounts(last_sync_time);