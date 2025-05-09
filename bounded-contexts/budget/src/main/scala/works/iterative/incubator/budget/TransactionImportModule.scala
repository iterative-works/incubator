package works.iterative.incubator.budget

import works.iterative.incubator.budget.domain.service.{
    BankTransactionService,
    TransactionImportService
}
import works.iterative.incubator.budget.domain.repository.{
    ImportBatchRepository,
    TransactionRepository
}
import works.iterative.incubator.budget.infrastructure.adapter.*
import works.iterative.incubator.budget.ui.transaction_import.{
    TransactionImportPresenter,
    MockTransactionImportPresenter,
    TransactionImportPresenterLive
}
import zio.*

/** Module providing all required dependencies for transaction import functionality.
  *
  * This object contains various ZLayer configurations for different scenarios:
  *   - Full implementation with real services (for production)
  *   - In-memory implementation for testing/development with mock bank service
  *   - UI-only mock implementation for UI development without domain services
  *
  * Category: Module Configuration Layer: Application
  */
object TransactionImportModule:
    /** Layer containing the mock presenter for UI development.
      *
      * This layer provides a standalone mock implementation that doesn't depend on any domain
      * services, useful for developing UI components in isolation.
      */
    val mockLayer: ULayer[TransactionImportPresenter] =
        MockTransactionImportPresenter.layer

    /** Layer containing the in-memory implementation for testing and development.
      *
      * This layer provides a fully functional implementation using in-memory repositories and a
      * mock bank service.
      */
    val inMemoryLayer: ULayer[TransactionImportPresenter] =
        ZLayer.make[TransactionImportPresenter](
            TransactionImportPresenterLive.layer,
            TransactionImportService.live,
            InMemoryTransactionRepository.layer,
            InMemoryImportBatchRepository.layer,
            MockBankTransactionService.layer
        )

    /** Layer for production use (not fully implemented yet).
      *
      * This layer would use real infrastructure implementations for repositories and bank service
      * integration.
      *
      * TODO: Replace in-memory repositories with real implementations. TODO: Replace mock bank
      * service with real bank API integration.
      */
    val liveLayer: ULayer[TransactionImportPresenter] =
        ZLayer.make[TransactionImportPresenter](
            TransactionImportPresenterLive.layer,
            TransactionImportService.live,
            InMemoryTransactionRepository.layer, // TODO: Replace with real implementation
            InMemoryImportBatchRepository.layer, // TODO: Replace with real implementation
            MockBankTransactionService.layer // TODO: Replace with real implementation
        )
end TransactionImportModule
