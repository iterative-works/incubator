package works.iterative.incubator.transactions.application.port

import zio.*

/**
 * Transaction Port
 *
 * Port interface for the Transaction Management context to communicate with external systems.
 * This defines the boundary between the Transaction context and other contexts.
 *
 * Application Port: This is a port interface defining cross-context communication.
 */
trait TransactionPort:
    /**
     * Submit processed transactions to external systems
     *
     * @param transactionIds The IDs of the transactions to submit
     * @param sourceAccountId The ID of the source account
     * @return A map of transaction IDs to submission results
     */
    def submitTransactions(
        transactionIds: Seq[String], 
        sourceAccountId: String
    ): Task[Map[String, TransactionSubmissionResult]]
    
    /**
     * Link a source account to an external account
     *
     * @param sourceAccountId The ID of the source account
     * @param externalAccountId The ID of the external account
     * @param externalSystemId The ID of the external system
     * @return Unit indicating successful linking
     */
    def linkSourceAccount(
        sourceAccountId: String, 
        externalAccountId: String,
        externalSystemId: String
    ): Task[Unit]
end TransactionPort

/**
 * Transaction Submission Result
 *
 * Represents the result of submitting a transaction to an external system
 */
sealed trait TransactionSubmissionResult
case class TransactionSubmissionSuccess(externalId: String) extends TransactionSubmissionResult
case class TransactionSubmissionFailure(error: Throwable) extends TransactionSubmissionResult