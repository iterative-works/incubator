package works.iterative.incubator.fio.infrastructure.client

import zio.*
import zio.json.*
import sttp.client4.*
import sttp.client4.httpclient.zio.HttpClientZioBackend
import sttp.model.StatusCode
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import works.iterative.incubator.fio.domain.model.*
import works.iterative.incubator.fio.domain.model.error.*
import works.iterative.incubator.fio.infrastructure.config.FioConfig
import works.iterative.incubator.fio.infrastructure.client.FioCodecs.given

/** HTTP client for Fio Bank API
  *
  * This client handles communication with Fio Bank's REST API, supporting both date range based
  * queries and ID-based delta queries.
  *
  * Classification: Infrastructure Client
  */
trait FioClient:
    /** Fetch transactions for a specific date range
      *
      * @param token
      *   API token for Fio Bank
      * @param from
      *   Start date (inclusive)
      * @param to
      *   End date (inclusive)
      * @return
      *   Response containing transaction data
      */
    def fetchTransactions(token: String, from: LocalDate, to: LocalDate): Task[FioResponse]

    /** Fetch new transactions since the last fetch for this token
      *
      * This uses the /rest/last endpoint which returns all transactions that have not been fetched
      * yet with this token
      *
      * @param token
      *   API token for Fio Bank
      * @return
      *   Response containing new transaction data
      */
    def fetchNewTransactions(token: String): Task[FioResponse]
    
    /** Set the bookmark (zarážka) to a specific date
      *
      * This allows controlling the starting point for the /last endpoint
      * to avoid fetching historical data that might require special authorization.
      *
      * @param token
      *   API token for Fio Bank
      * @param date
      *   Date to set as the bookmark
      * @return
      *   Unit indicating success
      */
    def setLastDate(token: String, date: LocalDate): Task[Unit]
end FioClient

/** Live implementation of FioClient using sttp
  *
  * Classification: Infrastructure Client Implementation
  */
case class FioClientLive(
    backend: Backend[Task],
    baseUrl: String = "https://fioapi.fio.cz/v1/rest"
) extends FioClient:
    private val dateFormat = DateTimeFormatter.ofPattern("yyyy-MM-dd")

    override def fetchTransactions(
        token: String,
        from: LocalDate,
        to: LocalDate
    ): Task[FioResponse] =
        val url =
            uri"$baseUrl/periods/$token/${from.format(dateFormat)}/${to.format(dateFormat)}/transactions.json"

        for
            _ <- ZIO.logDebug(s"Fetching transactions from $from to $to: ${url}")
            _ <- validateToken(token)
            response <- basicRequest
                .get(url)
                .response(asStringAlways)
                .send(backend)
            _ <- ZIO.logDebug(s"Received response with status code: ${response.code}")
            result <- handleResponse(response)
        yield result
        end for
    end fetchTransactions

    override def fetchNewTransactions(token: String): Task[FioResponse] =
        val url = uri"$baseUrl/last/$token/transactions.json"

        for
            _ <- ZIO.logDebug(s"Fetching new transactions using /last endpoint: ${url}")
            _ <- validateToken(token)
            response <- basicRequest
                .get(url)
                .response(asStringAlways)
                .send(backend)
            _ <- ZIO.logDebug(s"Received response with status code: ${response.code}")
            result <- handleResponse(response)
        yield result
        end for
    end fetchNewTransactions
    
    override def setLastDate(token: String, date: LocalDate): Task[Unit] =
        val url = uri"$baseUrl/set-last-date/$token/${date.format(dateFormat)}/"
        
        for
            _ <- ZIO.logDebug(s"Setting last date bookmark to ${date}: ${url}")
            _ <- validateToken(token)
            response <- basicRequest
                .get(url)
                .response(asStringAlways)
                .send(backend)
            _ <- ZIO.logDebug(s"Received response with status code: ${response.code}")
            _ <- response.code match
                case StatusCode.Ok => ZIO.unit
                case _ => ZIO.fail(FioNetworkError(new RuntimeException(
                    s"Failed to set bookmark: ${response.code} - ${response.body}"
                )))
        yield ()
        end for
    end setLastDate

    /** Validate token format and security
      */
    private def validateToken(token: String): Task[Unit] =
        if token.isEmpty then
            ZIO.fail(FioValidationError("Fio API token cannot be empty"))
        else if token.length < 32 then
            ZIO.fail(FioValidationError("Fio API token is too short"))
        else
            ZIO.unit

    /** Handle HTTP response and convert to domain type or appropriate error
      */
    private def handleResponse(response: Response[String]): Task[FioResponse] =
        response.code match
            case StatusCode.Ok =>
                ZIO.fromEither(response.body.fromJson[FioResponse])
                    .mapError(err => FioParsingError(s"Failed to parse Fio response: $err"))

            case StatusCode.Unauthorized =>
                ZIO.fail(FioAuthenticationError("Invalid Fio API token"))

            case StatusCode.BadRequest =>
                ZIO.fail(FioValidationError(s"Invalid request parameters: ${response.body}"))

            case StatusCode.NotFound =>
                ZIO.fail(FioResourceNotFoundError(s"Resource not found: ${response.body}"))

            case StatusCode.TooManyRequests =>
                ZIO.fail(FioRateLimitError("Rate limit exceeded for Fio API"))

            case code if code.isServerError =>
                ZIO.fail(FioServerError(s"Fio API server error (${code.code}): ${response.body}"))

            case _ =>
                ZIO.fail(FioNetworkError(new RuntimeException(
                    s"Unexpected response from Fio API: ${response.code} - ${response.body}"
                )))
end FioClientLive

object FioClient:
    val layer: ZLayer[Backend[Task], Config.Error, FioClient] =
        ZLayer {
            for
                config <- ZIO.config[FioConfig](FioConfig.config)
                backend <- ZIO.service[Backend[Task]]
            yield FioClientLive(backend, config.apiUrl)
        }

    val test: ZLayer[Backend[Task], Nothing, FioClient] =
        ZLayer {
            for
                backend <- ZIO.service[Backend[Task]]
            yield FioClientLive(backend)
        }

    def layerWithConfig(config: FioConfig): ZLayer[Backend[Task], Nothing, FioClientLive] = ZLayer {
        for
            backend <- ZIO.service[Backend[Task]]
        yield FioClientLive(backend, config.apiUrl)
    }

    val live: ZLayer[Any, Throwable, FioClient] =
        HttpClientZioBackend.layer() >>> layer

    def liveWithConfig(config: FioConfig): ZLayer[Any, Throwable, FioClientLive] =
        HttpClientZioBackend.layer() >>> layerWithConfig(config)

    val testLive: ZLayer[Any, Throwable, FioClient] =
        HttpClientZioBackend.layer() >>> test
end FioClient
