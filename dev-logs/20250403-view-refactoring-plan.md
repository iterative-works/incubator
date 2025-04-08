# View Refactoring Plan - 2025-04-03

## Current State

We've created a basic view preview system with mock implementations of the views. However, we need to use the actual production views to ensure:

1. We're working with the real code, not duplicates
2. Changes made in the preview reflect the real UI
3. We avoid maintaining two copies of the same views

## Refactoring Approach

### 1. Extract Views from Modules

The current module implementations tightly couple:
- Service methods (database operations)
- View methods (HTML rendering)
- Routes (HTTP endpoints)

We need to extract the views into reusable, standalone components that can be shared between:
- The production modules
- Our preview system

### 2. Implementation Plan

#### Step 1: Create View Traits with Dependencies

Create base traits that define the view interfaces:

```scala
// SourceAccountViews.scala
trait SourceAccountViews {
  def accountNotFound(accountId: Long): TypedTag[String]
  def sourceAccountList(accounts: Seq[SourceAccount], selectedStatus: String = "active"): TypedTag[String]
  def sourceAccountForm(account: Option[SourceAccount] = None): TypedTag[String]
  def sourceAccountDetail(account: SourceAccount): TypedTag[String]
}

// TransactionViews.scala
trait TransactionViews {
  def transactionList(
    transactions: Seq[TransactionWithState], 
    importStatus: Option[String] = None
  ): TypedTag[String]
}
```

#### Step 2: Extract Implementation Classes

Move the existing view implementations to standalone classes:

```scala
// SourceAccountViewsImpl.scala
class SourceAccountViewsImpl extends SourceAccountViews with ScalatagsSupport {
  import scalatags.Text.all._
  // Copy the existing view methods from SourceAccountModule.view
  // ...
}

// TransactionViewsImpl.scala
class TransactionViewsImpl extends TransactionViews with ScalatagsSupport {
  import scalatags.Text.all._
  // Copy the existing view methods from TransactionImportModule.view
  // ...
}
```

#### Step 3: Update the Modules to Use the Views

Modify the existing modules to use the extracted view implementations:

```scala
class SourceAccountModule(appShell: ScalatagsAppShell) extends ZIOWebModule[SourceAccountRepository] {
  // Inject the view implementation
  private val views: SourceAccountViews = new SourceAccountViewsImpl()
  
  // Keep the service methods
  object service {
    // Existing service methods...
  }
  
  // Routes now use views from the injected implementation
  override def routes: HttpRoutes[WebTask] = {
    // Use views.sourceAccountList instead of view.sourceAccountList
    // ...
  }
}
```

#### Step 4: Update the Preview System to Use the Same Views

Use the extracted view implementations in our preview system:

```scala
class ViewPreviewModule(appShell: ScalatagsAppShell) extends ZIOWebModule[ViewPreviewMain.PreviewEnv] {
  // Use the same view implementations as production
  private val sourceAccountViews: SourceAccountViews = new SourceAccountViewsImpl()
  private val transactionViews: TransactionViews = new TransactionViewsImpl()
  
  // Test data provider
  private val dataProvider = new TestDataProvider()
  
  // Routes for previewing the views with test data
  override def routes: HttpRoutes[PreviewTask] = {
    // ...
  }
}
```

### 3. Benefits of This Approach

1. **Single Source of Truth**: View logic is defined in one place only
2. **Separation of Concerns**: Views are independent of services and routes
3. **Testability**: Views can be tested in isolation with different data scenarios
4. **Reusability**: The same views can be used in multiple contexts
5. **Maintainability**: Changes to views are consistent across all usages

### 4. Implementation Timeline

1. Extract the view traits (1 hour)
2. Extract the implementation classes (2 hours)
3. Update the modules to use the extracted views (1 hour)
4. Update the preview system to use the same views (1 hour)
5. Create appropriate test data generators (2 hours)
6. Test and validate the refactoring (1 hour)

Total estimated time: 8 hours (1 day)

## Dependencies

To make this work, we need to consider component dependencies:

1. **ScalatagsAppShell**: Both modules currently use this for wrapping pages
2. **ScalatagsTailwindTable**: Used for table rendering
3. **Data Models**: Source account and transaction models

We need to ensure that both the production modules and preview system have access to these dependencies.

## Next Steps

1. Begin with extracting SourceAccountViews, as this has more view methods
2. Test with the preview system using sample data
3. Then extract and test TransactionViews
4. Document the approach and patterns for future UI development