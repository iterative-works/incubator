package works.iterative.incubator.transactions.web.module

import works.iterative.server.http.ZIOWebModule
import org.http4s.HttpRoutes
import org.http4s.dsl.Http4sDsl
import zio.interop.catz.*
import zio.*
import works.iterative.scalatags.ScalatagsSupport
import works.iterative.scalatags.components.ScalatagsAppShell
import scalatags.text.Frag
import org.http4s.Response
import org.http4s.Request
import cats.Monad
import cats.data.Kleisli
import cats.data.OptionT
import cats.syntax.all.*
import cats.Applicative
import works.iterative.incubator.transactions.domain.repository.TransactionRepository
import works.iterative.incubator.transactions.domain.repository.TransactionProcessingStateRepository
import works.iterative.incubator.transactions.application.service.TransactionProcessor
import works.iterative.incubator.transactions.application.service.TransactionManagerService
import java.time.LocalDate
import org.http4s.headers.Location
import org.http4s.Uri
import works.iterative.incubator.transactions.web.view.TransactionViews
import works.iterative.incubator.transactions.web.view.TransactionViewsImpl
import works.iterative.incubator.transactions.web.view.TransactionWithState
import works.iterative.incubator.transactions.domain.model.Transaction
import works.iterative.incubator.transactions.domain.model.TransactionId
import works.iterative.incubator.transactions.domain.model.TransactionProcessingState
import works.iterative.incubator.transactions.domain.query.TransactionQuery
import works.iterative.incubator.transactions.domain.query.TransactionProcessingStateQuery

/** Web module for transaction import and management
  *
  * This module provides UI for importing transactions, viewing them, and managing their processing
  * state.
  *
  * Classification: Web Module
  */
class TransactionImportModule(appShell: ScalatagsAppShell)
    extends ZIOWebModule[
        TransactionRepository & TransactionProcessingStateRepository &
            TransactionManagerService & TransactionProcessor
    ]
    with ScalatagsSupport:

    // Use the extracted view implementation
    private val views: TransactionViews = new TransactionViewsImpl()

    object service:
        // Get transactions with their processing states
        def getTransactionsWithState(using
            req: Request[WebTask]
        ): WebTask[(Seq[TransactionWithState], Option[String])] =
            for
                // Get all transactions
                transactions <- ZIO.serviceWithZIO[TransactionRepository](
                    _.find(TransactionQuery())
                )

                // Get all processing states
                processingStates <- ZIO.serviceWithZIO[TransactionProcessingStateRepository](
                    _.find(TransactionProcessingStateQuery())
                )

                // Group processing states by transaction ID for efficient lookup
                statesByTxId = processingStates.groupBy(_.transactionId)

                // Combine transactions with their states
                combined = transactions.map(tx =>
                    TransactionWithState(tx, statesByTxId.get(tx.id).flatMap(_.headOption))
                )
            yield (combined, req.params.get("importStatus"))

        // Import and process transactions for yesterday
        def importYesterdayTransactions: WebTask[String] =
            val today = LocalDate.now()
            val yesterday = today.minusDays(1)
            ZIO.serviceWithZIO[TransactionManagerService](
                _.importAndProcessTransactions(yesterday, today)
            ).fold(
                err => s"Failed to import transactions: $err",
                summary =>
                    s"Successfully imported ${summary.importedCount} transactions, " +
                        s"initialized ${summary.initializedCount}, " +
                        s"processed ${summary.processedCount}"
            )
        end importYesterdayTransactions

        // Process transactions with AI categorization
        def processWithAI(transactionIds: Seq[TransactionId]): WebTask[Int] =
            // For now we'll just call our processor to process all imported transactions
            ZIO.serviceWithZIO[TransactionProcessor](
                _.processImportedTransactions()
            ).catchAll(err =>
                ZIO.logError(s"Failed to process transactions: $err") *> ZIO.succeed(0)
            )

        // Submit transactions to YNAB
        def submitToYNAB(transactionIds: Seq[TransactionId])
            : WebTask[Seq[(Transaction, TransactionProcessingState)]] =
            // This is a stub that just gets transactions ready for submission
            // In a real implementation, you'd call a YNAB API client here
            ZIO.serviceWithZIO[TransactionProcessor](
                _.findTransactionsReadyForSubmission()
            ).catchAll(err =>
                ZIO.logError(
                    s"Failed to find transactions ready for submission: $err"
                ) *> ZIO.succeed(Seq.empty)
            )
    end service

    override def routes: HttpRoutes[WebTask] =
        val dsl = Http4sDsl[WebTask]
        import dsl.*

        def of[F[_]: Monad](pf: PartialFunction[Request[F], Request[F] => F[Response[F]]])
            : HttpRoutes[F] =
            Kleisli: req =>
                OptionT(Applicative[F].unit >> pf.lift(req).map(_(req)).sequence)

        def respondZIO[A](
            provide: Request[WebTask] ?=> WebTask[A],
            render: A => Frag
        ): Request[WebTask] => WebTask[Response[WebTask]] =
            req =>
                provide(using req).flatMap(data =>
                    Ok(appShell.wrap("Transactions", render(data)))
                )

        def respondZIOFull[A](
            provide: Request[WebTask] ?=> WebTask[A],
            reply: A => WebTask[Response[WebTask]]
        ): Request[WebTask] => WebTask[Response[WebTask]] =
            req =>
                provide(using req).flatMap(resp => reply(resp))

        of[WebTask] {
            // Main transaction listing page
            case GET -> Root / "transactions" =>
                respondZIO(
                    service.getTransactionsWithState,
                    (data, status) => views.transactionList(data, status)
                )

            // Handle the import-yesterday button submission
            case POST -> Root / "transactions" / "import-yesterday" =>
                respondZIOFull(
                    service.importYesterdayTransactions,
                    importStatus =>
                        // Redirect back to the transactions page with status message
                        SeeOther(Location(Uri.unsafeFromString("/transactions").withQueryParam(
                            "importStatus",
                            importStatus
                        )))
                )
        }
    end routes
end TransactionImportModule