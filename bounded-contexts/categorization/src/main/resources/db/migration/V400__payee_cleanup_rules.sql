-- Migration for payee_cleanup_rules table
-- This table stores rules for cleaning up payee names in transactions.

CREATE TABLE payee_cleanup_rules (
    id VARCHAR(36) PRIMARY KEY,
    pattern VARCHAR(255) NOT NULL,
    pattern_type VARCHAR(20) NOT NULL,
    replacement VARCHAR(255) NOT NULL,
    confidence DOUBLE PRECISION NOT NULL,
    generated_by VARCHAR(20) NOT NULL,
    status VARCHAR(20) NOT NULL,
    usage_count INTEGER NOT NULL DEFAULT 0,
    success_rate DOUBLE PRECISION NOT NULL DEFAULT 1.0,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL,
    updated_at TIMESTAMP WITH TIME ZONE
);

-- Index for efficient rule lookup
CREATE INDEX idx_payee_cleanup_rules_status ON payee_cleanup_rules(status);

-- Create table for tracking rule applications and feedback
CREATE TABLE payee_rule_applications (
    id SERIAL PRIMARY KEY,
    rule_id VARCHAR(36) NOT NULL REFERENCES payee_cleanup_rules(id),
    transaction_id VARCHAR(255) NOT NULL,
    original_payee VARCHAR(255) NOT NULL,
    cleaned_payee VARCHAR(255) NOT NULL,
    applied_at TIMESTAMP WITH TIME ZONE NOT NULL,
    feedback_status VARCHAR(20),
    feedback_at TIMESTAMP WITH TIME ZONE,
    CONSTRAINT unique_transaction_rule UNIQUE (transaction_id, rule_id)
);

-- Index for efficient lookup of rule applications by transaction
CREATE INDEX idx_payee_rule_applications_transaction ON payee_rule_applications(transaction_id);

-- Index for efficient lookup of rule applications by rule
CREATE INDEX idx_payee_rule_applications_rule ON payee_rule_applications(rule_id);

-- Comments
COMMENT ON TABLE payee_cleanup_rules IS 'Rules for cleaning up payee names in transactions';
COMMENT ON COLUMN payee_cleanup_rules.id IS 'Unique identifier for the rule';
COMMENT ON COLUMN payee_cleanup_rules.pattern IS 'Pattern to match against raw payee names';
COMMENT ON COLUMN payee_cleanup_rules.pattern_type IS 'Type of pattern: EXACT, CONTAINS, STARTS_WITH, REGEX';
COMMENT ON COLUMN payee_cleanup_rules.replacement IS 'The cleaned payee name to use';
COMMENT ON COLUMN payee_cleanup_rules.confidence IS 'Confidence score (0.0 to 1.0)';
COMMENT ON COLUMN payee_cleanup_rules.generated_by IS 'How this rule was generated: LLM, HUMAN';
COMMENT ON COLUMN payee_cleanup_rules.status IS 'Current status: PENDING, APPROVED, REJECTED';
COMMENT ON COLUMN payee_cleanup_rules.usage_count IS 'How many times rule has been applied';
COMMENT ON COLUMN payee_cleanup_rules.success_rate IS 'Success rate based on feedback (0.0 to 1.0)';
COMMENT ON COLUMN payee_cleanup_rules.created_at IS 'When this rule was created';
COMMENT ON COLUMN payee_cleanup_rules.updated_at IS 'When this rule was last updated';

COMMENT ON TABLE payee_rule_applications IS 'Track applications of payee cleanup rules to transactions';
COMMENT ON COLUMN payee_rule_applications.rule_id IS 'The rule that was applied';
COMMENT ON COLUMN payee_rule_applications.transaction_id IS 'The transaction ID the rule was applied to';
COMMENT ON COLUMN payee_rule_applications.original_payee IS 'The original payee name before cleanup';
COMMENT ON COLUMN payee_rule_applications.cleaned_payee IS 'The cleaned payee name after rule application';
COMMENT ON COLUMN payee_rule_applications.applied_at IS 'When the rule was applied';
COMMENT ON COLUMN payee_rule_applications.feedback_status IS 'User feedback: CORRECT, INCORRECT, null for no feedback';
COMMENT ON COLUMN payee_rule_applications.feedback_at IS 'When feedback was provided';