# Development Plan: YNAB API Integration

**Feature References**: 
- [Source Account Management](../ynab-importer/features/source_account_management.feature) - Specifically the "Link a source account to YNAB" scenario
- [YNAB Integration](../ynab-importer/features/ynab_integration.feature)

## Project Status

We've recently completed the implementation of the Create pattern for the SourceAccount repository, which provides a proper mechanism for generating IDs when creating new source accounts. According to our TODO list, our next priority is implementing the YNAB API integration.

## Problem

Currently, our application can manage source accounts and has the initial transaction entity structure, but lacks integration with YNAB. We need to:

1. Connect to YNAB API to retrieve account information
2. Allow mapping between bank accounts and YNAB accounts
3. Enable transaction submission to YNAB
4. Support YNAB category synchronization for AI categorization

## Solution Approach

Implement a multi-layered YNAB integration following our Functional Core/Imperative Shell architecture:

1. Core domain interfaces in the core module
2. API client implementation in the infrastructure module
3. UI components for YNAB account selection

### Implementation Plan

#### 1. YNAB Domain Models (Core Module)

```scala
// Domain entities for YNAB
case class YnabAccount(
    id: String,
    name: String,
    type: String,
    balance: BigDecimal,
    closed: Boolean
)

case class YnabCategory(
    id: String,
    name: String,
    groupId: String,
    groupName: String,
    hidden: Boolean
)

// Service interface
trait YnabService:
    def getAccounts(): Task[Seq[YnabAccount]]
    def getCategories(): Task[Seq[YnabCategory]]
    def submitTransaction(transaction: YnabTransaction): Task[String]
```

#### 2. YNAB API Client (Infrastructure Module)

```scala
// API client implementation using sttp or http4s
class YnabApiClient(config: YnabConfig, httpClient: SttpBackend[Task, Any]):
    def fetchAccounts(): Task[Seq[YnabApiAccount]] = ???
    def fetchCategories(): Task[Seq[YnabApiCategory]] = ???
    def createTransaction(transaction: YnabApiTransaction): Task[YnabApiResponse] = ???

// Implementation of the YnabService interface
class DefaultYnabService(client: YnabApiClient) extends YnabService:
    override def getAccounts(): Task[Seq[YnabAccount]] = 
        client.fetchAccounts().map(_.map(convertToYnabAccount))
    
    override def getCategories(): Task[Seq[YnabCategory]] =
        client.fetchCategories().map(_.map(convertToYnabCategory))
    
    override def submitTransaction(transaction: YnabTransaction): Task[String] =
        client.createTransaction(convertToApiTransaction(transaction))
            .map(_.id)
```

#### 3. UI Integration for Account Selection

Enhance the Source Account UI to include YNAB account selection:

```scala
// In SourceAccountModule
def accountForm(account: Option[SourceAccount] = None): Tag = 
    form(
        // Existing form fields
        
        // Add YNAB account dropdown
        div(
            label(`for` := "ynabAccountId")("YNAB Account"),
            select(
                id := "ynabAccountId",
                name := "ynabAccountId",
                loadYnabAccounts().map { account =>
                    option(
                        value := account.id,
                        account.name
                    )
                }
            )
        ),
        
        // Submit button
    )

// Helper method to load YNAB accounts
def loadYnabAccounts(): ZIO[YnabService, Throwable, Seq[YnabAccount]] =
    ZIO.serviceWithZIO[YnabService](_.getAccounts())
```

## Implementation Steps

1. Create YNAB core domain models
2. Design the YnabService interface
3. Create HTTP client implementation with authentication
4. Implement account and category synchronization
5. Integrate with SourceAccount UI
6. Add transaction submission capability
7. Create proper error handling and retry mechanisms
8. Add comprehensive tests

## Next Tasks in Order

1. Create YNAB configuration model with API key storage
2. Implement the YNAB API client using http4s or sttp
3. Create the YnabService implementation
4. Enhance the SourceAccount UI with YNAB account selection
5. Add unit and integration tests

This implementation will be guided by our functional core architecture principles, ensuring clean separation between domain logic and external integrations.