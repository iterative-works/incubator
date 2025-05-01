---
status: draft
last_updated: 2025-04-24
version: "0.1"
tags:
  - workflow
  - feature-specification
---
> [!info] Draft Document
> This document is an initial draft and may change significantly.

# Feature Specification: Fio Bank to YNAB Integration

## Feature Metadata
- **Related Change Request**: [CR-2025001](../change-requests/CR-2025001.md)
- **Feature ID**: BUDGET-001
- **Priority**: High
- **Target Release**: 2025-04-15
- **Business Domain**: Financial Data Management

## Feature Overview
### Description
The Fio Bank to YNAB Integration is a web-based tool that automates the import, categorization, and submission of financial transactions from Fio Bank to the YNAB (You Need A Budget) application. The system leverages AI for transaction categorization and provides a user interface for review and modification before submission.

### Business Value
This feature significantly reduces the manual effort required to manage financial data between Fio Bank and YNAB, improves categorization accuracy through AI assistance, and enables more timely financial reporting and budget management. By automating this process, users can focus on financial analysis rather than data entry.

### User Stories
- As a finance team member, I want to automatically import transactions from Fio Bank so that I don't have to manually export and import data.
- As a finance team member, I want transactions to be automatically categorized so that I don't have to manually assign categories to each transaction.
- As a finance team member, I want to review and modify categorizations before submission to YNAB so that I can ensure accuracy.
- As a finance team member, I want to view the status of all transactions so that I can track what has been imported, categorized, and submitted.
- As an administrator, I want to securely access the system so that financial data is protected.

## Detailed Requirements

### Functional Requirements
#### Core Requirements
- **FR1**: Fio Bank API Integration
  - Details: The system must authenticate with Fio Bank API using OAuth and retrieve transaction data for a specified date range.
  - Rationale: Automatic data retrieval eliminates manual export/import steps and reduces potential for human error.

- **FR2**: Transaction Storage
  - Details: All retrieved transactions must be stored in a PostgreSQL database with appropriate schema including transaction ID, date, amount, description, counterparty, and account information.
  - Rationale: Local storage enables processing, categorization, and tracking before submission to YNAB.

- **FR3**: AI Categorization
  - Details: The system must use the OpenAI API to analyze transaction descriptions and assign appropriate YNAB categories based on pattern recognition.
  - Rationale: Automated categorization saves significant time and improves consistency over manual categorization.

- **FR4**: Transaction Management Interface
  - Details: The web interface must display all transactions with their status (imported, categorized, submitted) and allow filtering, sorting, and searching.
  - Rationale: Users need visibility into the state of all transactions and tools to manage large transaction sets efficiently.

- **FR5**: Category Review and Modification
  - Details: Users must be able to review AI-assigned categories and modify them if necessary before submission to YNAB.
  - Rationale: Human oversight ensures accuracy and builds trust in the automated system.

- **FR6**: YNAB API Integration
  - Details: The system must authenticate with YNAB API and submit processed transactions to the correct account and budget.
  - Rationale: Automated submission eliminates another manual step in the workflow.

- **FR7**: Duplicate Prevention
  - Details: The system must track submitted transactions and prevent duplicate submissions to YNAB.
  - Rationale: Duplicate transactions would distort financial reports and budgets.

#### Optional Requirements
- **OFR1**: Transaction Rules Creation
  - Details: Allow users to create and save rules for automatic categorization of specific transaction patterns.
  - Conditions for Inclusion: If the AI categorization accuracy is below 85% after initial implementation.

- **OFR2**: Transaction Tags Support
  - Details: Support YNAB transaction tags in addition to categories.
  - Conditions for Inclusion: If development time allows after core requirements are met.

### Non-Functional Requirements
- **NFR1**: Categorization Accuracy
  - Measurement Criteria: AI categorization must achieve at least 80% accuracy compared to human-verified categories.

- **NFR2**: Performance
  - Measurement Criteria: Import and categorization of 100 transactions must complete within 60 seconds.

- **NFR3**: Usability
  - Measurement Criteria: Administrative users must be able to complete a full import-review-submit cycle with minimal training.

- **NFR4**: Compatibility
  - Measurement Criteria: Web interface must function correctly on latest versions of Chrome, Firefox, Safari, and Edge browsers.

- **NFR5**: Security
  - Measurement Criteria: All API keys and credentials must be securely stored; data encryption in transit and at rest.

### Constraints
- **Technical Constraints**: Must work within the limitations of Fio Bank and YNAB APIs.
- **Business Constraints**: Must be completed within 15 person-days of effort.
- **Regulatory/Compliance**: Must handle financial data in accordance with relevant data protection regulations.

## User Experience

### User Flow
1. User initiates a new import from Fio Bank, specifying date range
2. System retrieves transactions and stores them in database
3. System automatically categorizes transactions using AI
4. User reviews transaction list, with ability to filter, sort, and search
5. User reviews and modifies categories as needed
6. User selects transactions to submit to YNAB
7. System submits transactions and updates their status
8. User can view confirmation of submitted transactions

### UI/UX Requirements
- **Screen 1**: Dashboard
  - Elements:
    - Summary statistics (total transactions, categorized, submitted)
    - Import button with date range selector
    - Transaction table with status indicators
    - Filter and search controls
  - Behavior:
    - Dynamically updates statistics
    - Allows initiating new imports
    - Provides overview of system status

- **Screen 2**: Transaction Management
  - Elements:
    - Detailed transaction table with the following columns:
      - Date
      - Amount
      - Description
      - Counterparty
      - Category (with edit capability)
      - Status
      - Action buttons
    - Bulk action controls
    - Filter, sort, and search functionality
  - Behavior:
    - Allows in-line editing of categories
    - Supports multi-select for bulk actions
    - Updates transaction status in real-time

- **Screen 3**: Submission Confirmation
  - Elements:
    - List of submitted transactions
    - Success/failure indicators
    - YNAB link to view in budget
  - Behavior:
    - Shows detailed results of submission process
    - Allows navigation back to transaction management

### Wireframes/Mockups
[To be developed during UI design phase]

## Acceptance Criteria
### Scenario 1: Successful Import from Fio Bank
- **Given** I access the application
- **When** I initiate an import for the date range of April 1-15, 2025
- **Then** the system should connect to Fio Bank API
- **And** retrieve all transactions for the specified date range
- **And** store them in the database
- **And** display them in the transaction table with "Imported" status

### Scenario 2: AI Categorization of Transactions
- **Given** transactions have been imported from Fio Bank
- **When** the AI categorization process completes
- **Then** each transaction should have an assigned YNAB category
- **And** the transaction status should update to "Categorized"
- **And** at least 80% of common transaction types should have correct categories

### Scenario 3: Manual Category Modification
- **Given** I am viewing the transaction management screen
- **When** I click on the category field for a transaction
- **Then** I should see a dropdown of available YNAB categories
- **And** when I select a new category and save
- **Then** the transaction should update with the new category

### Scenario 4: Successful Submission to YNAB
- **Given** I have reviewed and categorized transactions
- **When** I select multiple transactions and click "Submit to YNAB"
- **Then** the system should connect to YNAB API
- **And** submit the selected transactions with their categories
- **And** update their status to "Submitted"
- **And** display a confirmation with the submission results

### Scenario 5: Duplicate Prevention
- **Given** transactions have been successfully submitted to YNAB
- **When** I attempt to submit the same transactions again
- **Then** the system should identify them as duplicates
- **And** prevent the resubmission
- **And** display an appropriate notification

## Technical Considerations

### Domain Analysis
- **Key Domain Concepts**:
  - Transaction: A financial movement recorded in Fio Bank
  - Category: YNAB classification for budgeting purposes
  - Import: Process of retrieving transactions from Fio Bank
  - Submission: Process of sending transactions to YNAB
- **Business Rules**:
  - Each transaction must have exactly one category before submission to YNAB
  - Transactions must not be submitted to YNAB more than once
  - All imported transactions must maintain an audit trail of status changes

### Integration Points
- **External Systems**:
  - Fio Bank API: OAuth-based authentication, JSON response format
  - YNAB API: Token-based authentication, REST endpoints for transaction submission
  - OpenAI API: Token-based authentication, completion API for categorization
- **Internal Components**:
  - PostgreSQL Database: Transaction storage and status tracking
  - Authentication Service: JWT token generation and validation
  - Background Processing: For handling longer-running import and categorization tasks

### Data Requirements
- **Data Entities**:
  - Transaction:
    - Attributes: id, date, amount, description, counterparty, account_id, import_batch_id
    - Relationships: belongs to Import Batch, has one Category
  - Category:
    - Attributes: id, name, ynab_id, confidence_score
    - Relationships: has many Transactions
  - Import Batch:
    - Attributes: id, date_range_start, date_range_end, status, created_at
    - Relationships: has many Transactions

- **Data Sources**:
  - Fio Bank API: Primary source of transaction data
  - YNAB API: Source of category definitions
  - Local database: Storage for processing state

### Performance Considerations
- **Expected Load**: Up to 1,000 transactions per month
- **Response Time Requirements**: User interface actions should complete within 2 seconds
- **Throughput Requirements**: Import process should handle at least 10 transactions per second

## Testing Considerations

### Test Strategy
- **Unit Testing Approach**: ZIO tests
- **Integration Testing Approach**: ZIO tests
- **End-to-End Testing Approach**: Playwright in ZIO tests

### Test Cases
- **Test Case 1**: Fio Bank API Connection
  - Preconditions: Valid API credentials configured
  - Steps: Initiate connection to Fio Bank API, retrieve transactions for test period
  - Expected Results: Connection successful, transactions retrieved and match expected format

- **Test Case 2**: AI Categorization Accuracy
  - Preconditions: Test dataset of 100 transactions with known correct categories
  - Steps: Run AI categorization on test dataset
  - Expected Results: At least 80 transactions assigned the correct category

- **Test Case 3**: End-to-End Transaction Flow
  - Preconditions: System configured with test accounts for both Fio Bank and YNAB
  - Steps: Import transactions, review categories, submit to YNAB
  - Expected Results: Transactions appear in YNAB with correct details and categories

## Implementation Guidance

### High-Level Architecture
The system will follow a three-tier architecture with a Scala backend using ZIO, Scalatags for HTML templating combined with HTMX for interactive frontend capabilities, and PostgreSQL database. Background processing will leverage ZIO's concurrency features for handling longer-running tasks.

### Key Components
- **Core ZIO Application**:
  - Purpose: Serve as the central processing hub with modular effects
  - Responsibilities: Business logic coordination, request handling, effect management

- **HTTP Layer**:
  - Purpose: Handle web requests and serve HTML/HTMX interface
  - Responsibilities: API endpoints, Scalatags templating, HTMX integration, TailwindCSS integration

- **Integration Services**:
  - Purpose: Connect with external systems (Fio Bank, YNAB, OpenAI)
  - Responsibilities: API authentication, data transformation, error handling

- **Persistent Layer**:
  - Purpose: Manage data storage and retrieval
  - Responsibilities: PostgreSQL interactions, transaction history, state management

- **Transaction Processing Pipeline**:
  - Purpose: Orchestrate the flow of transaction data through the system
  - Responsibilities: Import scheduling, categorization, submission to YNAB

### Implementation Recommendations
- Implement robust error handling and logging, especially for external API interactions
- Create a separate service for AI categorization to allow independent scaling and evolution
- Use database transactions to ensure data consistency during critical operations

### Implementation Risks
- **External API Limitations**: Fio Bank or YNAB may impose rate limits or have downtime
  - Mitigation: Implement retry logic and graceful error handling

- **AI Categorization Accuracy**: Initial categorization may not meet accuracy targets
  - Mitigation: Implement feedback mechanism to improve categorization over time

- **Data Security**: Financial data requires stringent security measures
  - Mitigation: Implement proper encryption, authentication, and authorization throughout

## Approval and Sign-off

### Product Approval
- **Product Owner**: Michal Příhoda
- **Approval Date**: 2025-05-01
- **Comments**: Let's go.

### Technical Approval
- **Tech Lead/Architect**: Michal Příhoda
- **Approval Date**: 2025-05-01
- **Comments**: I'm in.

## Document History

| Version | Date | Changes | Author |
| ----------- | -------- | ------------- | ---------- |
| 0.1 | 2025-04-18 | Initial draft | AI |
| 0.2 | 2025-05-01 | Human review | Michal Příhoda |
