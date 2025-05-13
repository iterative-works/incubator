CREATE TABLE import_batches (
  id VARCHAR(100) PRIMARY KEY,
  account_id VARCHAR(50) NOT NULL,
  start_date DATE NOT NULL,
  end_date DATE NOT NULL,
  status VARCHAR(20) NOT NULL,
  transaction_count INTEGER NOT NULL DEFAULT 0,
  error_message TEXT,
  start_time TIMESTAMP WITH TIME ZONE NOT NULL,
  end_time TIMESTAMP WITH TIME ZONE,
  created_at TIMESTAMP WITH TIME ZONE NOT NULL,
  updated_at TIMESTAMP WITH TIME ZONE NOT NULL
);

-- Indexes for common query patterns
CREATE INDEX idx_import_batches_account_id
  ON import_batches(account_id);
CREATE INDEX idx_import_batches_dates
  ON import_batches(start_date, end_date);
CREATE INDEX idx_import_batches_status
  ON import_batches(status);