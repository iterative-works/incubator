package works.iterative.incubator.transactions
package infrastructure
package adapter.fio

import zio.*
import zio.json.*
import sttp.client3.*
import sttp.client3.httpclient.zio.HttpClientZioBackend
import sttp.model.Uri
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import FioCodecs.given

trait FioClient:
    def fetchTransactions(from: LocalDate, to: LocalDate): Task[FioResponse]
    def fetchNewTransactions(lastId: Long): Task[FioResponse]

case class FioClientLive(token: String, backend: SttpBackend[Task, Any]) extends FioClient:
    private val baseUrl = "https://www.fio.cz/ib_api/rest"
    private val dateFormat = DateTimeFormatter.ofPattern("yyyy-MM-dd")

    override def fetchTransactions(from: LocalDate, to: LocalDate): Task[FioResponse] =
        val url = Uri.parse(
            s"$baseUrl/periods/$token/${from.format(dateFormat)}/${to.format(dateFormat)}/transactions.json"
        )
            .getOrElse(throw new IllegalArgumentException("Invalid URL"))

        for
            response <- basicRequest
                .get(url)
                .response(asStringAlways)
                .send(backend)
            _ <- ZIO.logInfo(s"Received response from FIO API: ${response.body}")
            parsed <- ZIO.fromEither(response.body.fromJson[FioResponse])
                .mapError(err => new RuntimeException(s"Failed to parse Fio response: $err"))
        yield parsed
        end for
    end fetchTransactions

    override def fetchNewTransactions(lastId: Long): Task[FioResponse] =
        val url = Uri.parse(s"$baseUrl/by-id/$token/$lastId/transactions.json")
            .getOrElse(throw new IllegalArgumentException("Invalid URL"))

        for
            response <- basicRequest
                .get(url)
                .response(asStringAlways)
                .send(backend)
            parsed <- ZIO.fromEither(response.body.fromJson[FioResponse])
                .mapError(err => new RuntimeException(s"Failed to parse Fio response: $err"))
        yield parsed
        end for
    end fetchNewTransactions
end FioClientLive

object FioClient:
    val layer: ZLayer[SttpBackend[Task, Any], Config.Error, FioClient] =
        ZLayer {
            for
                config <- ZIO.config[FioConfig](FioConfig.config)
                backend <- ZIO.service[SttpBackend[Task, Any]]
            yield FioClientLive(config.token, backend)
        }

    val live: ZLayer[Any, Throwable, FioClient] =
        HttpClientZioBackend.layer() >>> layer
end FioClient
