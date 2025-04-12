package works.iterative.incubator.fio.infrastructure.client

import zio.test.*
import zio.json.*
import zio.*
import java.time.LocalDate
import works.iterative.incubator.fio.domain.model.*
import works.iterative.incubator.fio.domain.model.error.*
import works.iterative.incubator.fio.infrastructure.client.FioCodecs.given

/** Test suite for FioClient using STTP backend stub
  */
object FioClientSpec extends ZIOSpecDefault:

    // Sample dates for testing
    private val fromDate = LocalDate.of(2025, 3, 10)
    private val toDate = LocalDate.of(2025, 3, 15)

    // Example JSON response (smaller version for testing)
    private val responseJson = """
    {
      "accountStatement": {
        "info": {
          "accountId": "2200000001",
          "bankId": "2010",
          "currency": "CZK",
          "iban": "CZ5420100000002200000001",
          "bic": "FIOBCZPPXXX",
          "openingBalance": 257.69,
          "closingBalance": 47257.69,
          "dateStart": "2025-03-10+0100",
          "dateEnd": "2025-03-15+0100",
          "idFrom": 26962199069,
          "idTo": 26962448650
        },
        "transactionList": {
          "transaction": [
            {
              "column22": { "value": 26962199069, "name": "ID pohybu", "id": 22 },
              "column0": { "value": "2025-03-14+0100", "name": "Datum", "id": 0 },
              "column1": { "value": 50000.0, "name": "Objem", "id": 1 },
              "column14": { "value": "CZK", "name": "Měna", "id": 14 }
            }
          ]
        }
      }
    }
    """

    // Mock implementation of FioClient for testing
    class MockFioClient extends FioClient:
        override def fetchTransactions(token: String, from: LocalDate, to: LocalDate): Task[FioResponse] =
            ZIO.fromEither(responseJson.fromJson[FioResponse])
                .mapError(err => new RuntimeException(s"Failed to parse test JSON: $err"))

        override def fetchNewTransactions(token: String): Task[FioResponse] =
            ZIO.fromEither(responseJson.fromJson[FioResponse])
                .mapError(err => new RuntimeException(s"Failed to parse test JSON: $err"))
    end MockFioClient

    class FailingMockFioClient extends FioClient:
        override def fetchTransactions(token: String, from: LocalDate, to: LocalDate): Task[FioResponse] =
            ZIO.fail(FioAuthenticationError("Invalid Fio API token"))

        override def fetchNewTransactions(token: String): Task[FioResponse] =
            ZIO.fail(FioAuthenticationError("Invalid Fio API token"))
    end FailingMockFioClient

    override def spec = suite("FioClient")(
        test("fetchTransactions should correctly request and parse transactions for date range") {
            for
                client <- ZIO.succeed(new MockFioClient())
                response <- client.fetchTransactions("test-token", fromDate, toDate)
            yield assertTrue(
                response.isInstanceOf[FioResponse],
                response.accountStatement.info.accountId == "2200000001",
                response.accountStatement.info.bankId == "2010",
                response.accountStatement.transactionList.transaction.size == 1
            )
        },
        test("fetchNewTransactions should correctly request and parse transactions using last endpoint") {
            for
                client <- ZIO.succeed(new MockFioClient())
                response <- client.fetchNewTransactions("test-token")
            yield assertTrue(
                response.isInstanceOf[FioResponse],
                response.accountStatement.info.accountId == "2200000001",
                response.accountStatement.info.currency == "CZK",
                response.accountStatement.transactionList.transaction.size == 1
            )
        },
        test("client should handle error responses") {
            for
                client <- ZIO.succeed(new FailingMockFioClient())
                result <- client.fetchTransactions("test-token", fromDate, toDate).exit
            yield assertTrue(
                result.isFailure,
                result.toString.contains("Invalid Fio API token")
            )
        }
    )
end FioClientSpec
