package works.iterative.incubator.fio.infrastructure.client

import zio.test.*
import zio.test.Assertion.*
import zio.json.*
import zio.*
import sttp.client4.*
import sttp.client4.testing.SttpBackendStub
import sttp.model.StatusCode
import java.time.LocalDate
import works.iterative.incubator.fio.domain.model.*
import works.iterative.incubator.fio.infrastructure.client.FioCodecs.given

/**
 * Test suite for FioClient using STTP backend stub
 */
object FioClientSpec extends ZIOSpecDefault:

    // Token for testing
    private val testToken = "test-token"

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

    // Error response
    private val errorJson = """
    {"error": "Invalid token"}
    """

    // Test backend stub with predefined responses
    private val testBackend = SttpBackendStub.synchronous
        // Respond to date range endpoint
        .whenRequestMatches(request => 
            request.uri.toString.contains("/periods/") && 
            request.uri.toString.contains(testToken) &&
            request.uri.toString.contains(fromDate.toString) &&
            request.uri.toString.contains(toDate.toString)
        )
        .thenRespond(responseJson)
        
        // Respond to transaction ID endpoint
        .whenRequestMatches(request => 
            request.uri.toString.contains("/by-id/") && 
            request.uri.toString.contains(testToken) &&
            request.uri.toString.contains("123456")
        )
        .thenRespond(responseJson)
        
        // Invalid token response
        .whenRequestMatches(request => 
            request.uri.toString.contains("invalid-token")
        )
        .thenRespond(
            Response(errorJson, StatusCode.Unauthorized)
        )

    // Layer with stubbed backend
    private val testLayer = ZLayer.succeed(testBackend)

    // Test client
    private val testClient = FioClientLive(testToken, testBackend)

    override def spec = suite("FioClient")(
        test("fetchTransactions should correctly request and parse transactions for date range") {
            for
                response <- testClient.fetchTransactions(fromDate, toDate)
            yield
                assert(response)(isA[FioResponse]) &&
                assert(response.accountStatement.info.accountId)(equalTo("2200000001")) &&
                assert(response.accountStatement.info.bankId)(equalTo("2010")) &&
                assert(response.accountStatement.transactionList.transaction.size)(equalTo(1))
        },

        test("fetchNewTransactions should correctly request and parse transactions by ID") {
            for
                response <- testClient.fetchNewTransactions(123456L)
            yield
                assert(response)(isA[FioResponse]) &&
                assert(response.accountStatement.info.accountId)(equalTo("2200000001")) &&
                assert(response.accountStatement.info.currency)(equalTo("CZK")) &&
                assert(response.accountStatement.transactionList.transaction.size)(equalTo(1))
        },

        test("client should handle error responses") {
            val invalidClient = FioClientLive("invalid-token", testBackend)
            
            for
                result <- invalidClient.fetchTransactions(fromDate, toDate).exit
            yield
                assert(result)(fails(hasMessage(containsString("Failed to parse Fio response"))))
        }
    )