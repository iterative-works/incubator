package works.iterative.incubator.budget.infrastructure.adapter.fio

import works.iterative.incubator.budget.infrastructure.adapter.fio.FioModels.*
import zio.*
import zio.json.*
import sttp.client3.*
import sttp.client3.httpclient.zio.HttpClientZioBackend
import sttp.model.{StatusCode, Uri}
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import scala.concurrent.duration.Duration

/** Client for interacting with Fio Bank API.
  *
  * Provides methods for fetching transactions and handling API responses.
  *
  * Category: HTTP Client Layer: Infrastructure
  */
trait FioApiClient:
    /** Fetches transactions for a specific date range.
      *
      * @param token
      *   The Fio API token for authentication
      * @param dateFrom
      *   The start date for the transaction range
      * @param dateTo
      *   The end date for the transaction range
      * @return
      *   A ZIO effect that completes with a list of Fio transactions or fails with an error
      */
    def fetchTransactionsByDateRange(
        token: String,
        dateFrom: LocalDate,
        dateTo: LocalDate
    ): ZIO[Any, Throwable, List[FioTransaction]]

    /** Fetches new transactions since the last sync.
      *
      * @param token
      *   The Fio API token for authentication
      * @return
      *   A ZIO effect that completes with a list of Fio transactions or fails with an error
      */
    def fetchNewTransactions(token: String): ZIO[Any, Throwable, List[FioTransaction]]

    /** Sets the last date for the API bookmark.
      *
      * This marks all transactions up to this date as processed, so they won't be returned by the
      * fetchNewTransactions method.
      *
      * @param token
      *   The Fio API token for authentication
      * @param date
      *   The date to set as the bookmark
      * @return
      *   A ZIO effect that completes with Unit or fails with an error
      */
    def setLastDate(token: String, date: LocalDate): ZIO[Any, Throwable, Unit]
end FioApiClient

/** Implementation of FioApiClient using STTP.
  *
  * @param config
  *   The configuration for the Fio API client
  */
final case class FioApiClientLive(config: FioConfig) extends FioApiClient:
    private val dateFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd")

    /** Fetches transactions for a specific date range.
      *
      * @param token
      *   The Fio API token for authentication
      * @param dateFrom
      *   The start date for the transaction range
      * @param dateTo
      *   The end date for the transaction range
      * @return
      *   A ZIO effect that completes with a list of Fio transactions or fails with an error
      */
    override def fetchTransactionsByDateRange(
        token: String,
        dateFrom: LocalDate,
        dateTo: LocalDate
    ): ZIO[Any, Throwable, List[FioTransaction]] =
        val dateFromStr = dateFrom.format(dateFormatter)
        val dateToStr = dateTo.format(dateFormatter)
        val url = s"${config.baseUrl}/periods/$token/$dateFromStr/$dateToStr/transactions.json"

        executeRequest(url).map { response =>
            response.accountStatement.transactionList.transaction
        }
    end fetchTransactionsByDateRange

    /** Fetches new transactions since the last sync.
      *
      * @param token
      *   The Fio API token for authentication
      * @return
      *   A ZIO effect that completes with a list of Fio transactions or fails with an error
      */
    override def fetchNewTransactions(token: String): ZIO[Any, Throwable, List[FioTransaction]] =
        val url = s"${config.baseUrl}/last/$token/transactions.json"

        executeRequest(url).map { response =>
            response.accountStatement.transactionList.transaction
        }
    end fetchNewTransactions

    /** Sets the last date for the API bookmark.
      *
      * This marks all transactions up to this date as processed, so they won't be returned by the
      * fetchNewTransactions method.
      *
      * @param token
      *   The Fio API token for authentication
      * @param date
      *   The date to set as the bookmark
      * @return
      *   A ZIO effect that completes with Unit or fails with an error
      */
    override def setLastDate(token: String, date: LocalDate): ZIO[Any, Throwable, Unit] =
        val dateStr = date.format(dateFormatter)
        val url = s"${config.baseUrl}/set-last-date/$token/$dateStr/"

        ZIO.scoped {
            HttpClientZioBackend.scoped().flatMap { backend =>
                val request = basicRequest
                    .get(Uri.unsafeParse(url))
                    .response(asString)
                    .readTimeout(Duration(
                        config.requestTimeoutSeconds,
                        scala.concurrent.duration.SECONDS
                    ))

                ZIO.logDebug(s"Sending request to Fio API: $url") *>
                    request
                        .send(backend)
                        .tapError(e =>
                            ZIO.logError(s"Error connecting to Fio API: ${e.getMessage}")
                        )
                        .retry(FioConfig.retrySchedule(config))
                        .flatMap { response =>
                            response.code match
                                case StatusCode.Ok => ZIO.unit
                                case StatusCode.Conflict =>
                                    ZIO.fail(
                                        new RuntimeException(
                                            "Rate limit exceeded. Only 1 request per 30 seconds is allowed."
                                        )
                                    )
                                case StatusCode.Unauthorized =>
                                    ZIO.fail(new RuntimeException("Invalid token"))
                                case StatusCode.BadRequest =>
                                    ZIO.fail(new RuntimeException("Invalid request parameters"))
                                case _ =>
                                    ZIO.fail(
                                        new RuntimeException(
                                            s"Fio API error: ${response.code} - ${response.body.toString}"
                                        )
                                    )
                        }
            }
        }
    end setLastDate

    /** Executes a request to the Fio API and parses the JSON response.
      *
      * @param url
      *   The URL to request
      * @return
      *   A ZIO effect that completes with the parsed FioResponse or fails with an error
      */
    private def executeRequest(url: String): ZIO[Any, Throwable, FioResponse] =
        ZIO.scoped {
            HttpClientZioBackend.scoped().flatMap { backend =>
                val request = basicRequest
                    .get(Uri.unsafeParse(url))
                    .response(asString)
                    .readTimeout(Duration(
                        config.requestTimeoutSeconds,
                        scala.concurrent.duration.SECONDS
                    ))

                ZIO.logDebug(s"Sending request to Fio API: $url") *>
                    request
                        .send(backend)
                        .tapError(e =>
                            ZIO.logError(s"Error connecting to Fio API: ${e.getMessage}")
                        )
                        .retry(FioConfig.retrySchedule(config))
                        .flatMap { response =>
                            response.code match
                                case StatusCode.Ok =>
                                    response.body match
                                        case Right(body) =>
                                            ZIO.fromEither(body.fromJson[FioResponse])
                                                .mapError(err =>
                                                    new RuntimeException(s"JSON parse error: $err")
                                                )
                                        case Left(error) =>
                                            ZIO.fail(
                                                new RuntimeException(s"HTTP response error: $error")
                                            )
                                case StatusCode.Conflict =>
                                    ZIO.fail(
                                        new RuntimeException(
                                            "Rate limit exceeded. Only 1 request per 30 seconds is allowed."
                                        )
                                    )
                                case StatusCode.Unauthorized =>
                                    ZIO.fail(new RuntimeException("Invalid token"))
                                case StatusCode.BadRequest =>
                                    ZIO.fail(new RuntimeException("Invalid request parameters"))
                                case _ =>
                                    ZIO.fail(
                                        new RuntimeException(
                                            s"Fio API error: ${response.code} - ${response.body.toString}"
                                        )
                                    )
                        }
            }
        }
end FioApiClientLive

/** Companion object for FioApiClient.
  */
object FioApiClient:
    /** Accesses the client to fetch transactions by date range.
      *
      * @param token
      *   The Fio API token for authentication
      * @param dateFrom
      *   The start date for the transaction range
      * @param dateTo
      *   The end date for the transaction range
      * @return
      *   A ZIO effect that requires FioApiClient and completes with a list of Fio transactions or
      *   fails with an error
      */
    def fetchTransactionsByDateRange(
        token: String,
        dateFrom: LocalDate,
        dateTo: LocalDate
    ): ZIO[FioApiClient, Throwable, List[FioTransaction]] =
        ZIO.serviceWithZIO(_.fetchTransactionsByDateRange(token, dateFrom, dateTo))

    /** Accesses the client to fetch new transactions.
      *
      * @param token
      *   The Fio API token for authentication
      * @return
      *   A ZIO effect that requires FioApiClient and completes with a list of Fio transactions or
      *   fails with an error
      */
    def fetchNewTransactions(token: String): ZIO[FioApiClient, Throwable, List[FioTransaction]] =
        ZIO.serviceWithZIO(_.fetchNewTransactions(token))

    /** Accesses the client to set the last date for the API bookmark.
      *
      * @param token
      *   The Fio API token for authentication
      * @param date
      *   The date to set as the bookmark
      * @return
      *   A ZIO effect that requires FioApiClient and completes with Unit or fails with an error
      */
    def setLastDate(token: String, date: LocalDate): ZIO[FioApiClient, Throwable, Unit] =
        ZIO.serviceWithZIO(_.setLastDate(token, date))

    /** Creates a layer with the provided configuration.
      *
      * @return
      *   A ZLayer that provides a FioApiClient
      */
    val live: ZLayer[Any, Config.Error, FioApiClient] =
        ZLayer {
            for
                config <- ZIO.config[FioConfig]
            yield FioApiClientLive(config)
        }
end FioApiClient
