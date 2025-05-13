CREATE TABLE transactions (
  id VARCHAR(100) PRIMARY KEY,
  source_account_id VARCHAR(50) NOT NULL,
  transaction_date DATE NOT NULL,
  amount_value DECIMAL(19, 4) NOT NULL,
  amount_currency VARCHAR(3) NOT NULL,
  description TEXT NOT NULL,
  counterparty TEXT,
  counter_account TEXT,
  reference TEXT,
  import_batch_id VARCHAR(100) NOT NULL,
  status VARCHAR(20) NOT NULL,
  created_at TIMESTAMP WITH TIME ZONE NOT NULL,
  updated_at TIMESTAMP WITH TIME ZONE NOT NULL,
  FOREIGN KEY (import_batch_id) REFERENCES import_batches(id)
);

-- Indexes for common query patterns
CREATE INDEX idx_transactions_source_account_date
  ON transactions(source_account_id, transaction_date);
CREATE INDEX idx_transactions_import_batch
  ON transactions(import_batch_id);
CREATE INDEX idx_transactions_status
  ON transactions(status);