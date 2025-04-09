package works.iterative.incubator.fio.infrastructure.client

import zio.*
import zio.json.*
import sttp.client4.*
import sttp.client4.httpclient.zio.HttpClientZioBackend
import sttp.model.{Uri, StatusCode}
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
      * @param from
      *   Start date (inclusive)
      * @param to
      *   End date (inclusive)
      * @return
      *   Response containing transaction data
      */
    def fetchTransactions(from: LocalDate, to: LocalDate): Task[FioResponse]

    /** Fetch new transactions since a specific transaction ID
      *
      * @param lastId
      *   Last transaction ID that was processed
      * @return
      *   Response containing new transaction data
      */
    def fetchNewTransactions(lastId: Long): Task[FioResponse]
end FioClient

/** Live implementation of FioClient using sttp
  *
  * Classification: Infrastructure Client Implementation
  */
case class FioClientLive(token: String, backend: Backend[Task]) extends FioClient:
    private val baseUrl = "https://www.fio.cz/ib_api/rest"
    private val dateFormat = DateTimeFormatter.ofPattern("yyyy-MM-dd")

    override def fetchTransactions(from: LocalDate, to: LocalDate): Task[FioResponse] =
        val url = Uri.parse(
            s"$baseUrl/periods/$token/${from.format(dateFormat)}/${to.format(dateFormat)}/transactions.json"
        )
            .getOrElse(throw new IllegalArgumentException("Invalid URL"))

        for
            _ <- ZIO.logDebug(s"Fetching transactions from $from to $to")
            response <- basicRequest
                .get(url)
                .response(asStringAlways)
                .send(backend)
            _ <- ZIO.logDebug(s"Received response with status code: ${response.code}")
            result <- handleResponse(response)
        yield result
        end for
    end fetchTransactions

    override def fetchNewTransactions(lastId: Long): Task[FioResponse] =
        val url = Uri.parse(s"$baseUrl/by-id/$token/$lastId/transactions.json")
            .getOrElse(throw new IllegalArgumentException("Invalid URL"))

        for
            _ <- ZIO.logDebug(s"Fetching transactions since ID $lastId")
            response <- basicRequest
                .get(url)
                .response(asStringAlways)
                .send(backend)
            _ <- ZIO.logDebug(s"Received response with status code: ${response.code}")
            result <- handleResponse(response)
        yield result
        end for
    end fetchNewTransactions
    
    /**
     * Handle HTTP response and convert to domain type or appropriate error
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
            yield FioClientLive(config.token, backend)
        }

    def layerWithConfig(config: FioConfig): ZLayer[Backend[Task], Nothing, FioClientLive] = ZLayer {
        for
            backend <- ZIO.service[Backend[Task]]
        yield FioClientLive(config.token, backend)
    }

    val live: ZLayer[Any, Throwable, FioClient] =
        HttpClientZioBackend.layer() >>> layer

    def liveWithConfig(config: FioConfig): ZLayer[Any, Throwable, FioClientLive] =
        HttpClientZioBackend.layer() >>> layerWithConfig(config)
end FioClient