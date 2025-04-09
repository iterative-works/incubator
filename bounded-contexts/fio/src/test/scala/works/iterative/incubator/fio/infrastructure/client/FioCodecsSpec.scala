package works.iterative.incubator.fio.infrastructure.client

import zio.test.*
import zio.json.*
import zio.nio.file.{Files, Path}
import works.iterative.incubator.fio.domain.model.*
import works.iterative.incubator.fio.infrastructure.client.FioCodecs.given

/**
 * Test suite for Fio Bank API JSON decoders
 */
object FioCodecsSpec extends ZIOSpecDefault:

    val exampleJsonFile = "bounded-contexts/fio/src/test/resources/example_fio.json"

    override def spec = suite("FioCodecs")(
        test("should correctly parse Fio API response") {
            for
                jsonBytes <- Files.readAllBytes(Path(exampleJsonFile))
                jsonString = new String(jsonBytes.toArray)
                parsed <- zio.ZIO.fromEither(jsonString.fromJson[FioResponse])
            yield
                assertTrue(
                    parsed.isInstanceOf[FioResponse],
                    parsed.accountStatement.info.accountId == "2200000001",
                    parsed.accountStatement.info.bankId == "2010",
                    parsed.accountStatement.info.currency == "CZK",
                    parsed.accountStatement.info.iban == "CZ5420100000002200000001",
                    parsed.accountStatement.info.bic == "FIOBCZPPXXX",
                    parsed.accountStatement.transactionList.transaction.size == 2
                )
        },

        test("should correctly parse transaction values") {
            for
                jsonBytes <- Files.readAllBytes(Path(exampleJsonFile))
                jsonString = new String(jsonBytes.toArray)
                parsed <- zio.ZIO.fromEither(jsonString.fromJson[FioResponse])
                tx = parsed.accountStatement.transactionList.transaction.head
            yield
                assertTrue(
                    tx.column22.map(_.value).contains(26962199069L),
                    tx.column0.map(_.value).contains("2025-03-14+0100"),
                    tx.column1.map(_.value).contains(50000.0)
                )
        },

        test("should correctly handle null fields") {
            for
                jsonBytes <- Files.readAllBytes(Path(exampleJsonFile))
                jsonString = new String(jsonBytes.toArray)
                parsed <- zio.ZIO.fromEither(jsonString.fromJson[FioResponse])
                tx = parsed.accountStatement.transactionList.transaction.head
            yield
                assertTrue(
                    tx.column4.isEmpty,
                    tx.column5.isEmpty,
                    tx.column6.isEmpty
                )
        },

        test("should handle transaction fields for both transactions") {
            for
                jsonBytes <- Files.readAllBytes(Path(exampleJsonFile))
                jsonString = new String(jsonBytes.toArray)
                parsed <- zio.ZIO.fromEither(jsonString.fromJson[FioResponse])
                tx1 = parsed.accountStatement.transactionList.transaction(0)
                tx2 = parsed.accountStatement.transactionList.transaction(1)
            yield
                assertTrue(
                    // First transaction assertions
                    tx1.column1.map(_.value).contains(50000.0),
                    tx1.column8.map(_.value).contains("Příjem převodem uvnitř banky"),
                    
                    // Second transaction assertions
                    tx2.column1.map(_.value).contains(-3000.0),
                    tx2.column8.map(_.value).contains("Bezhotovostní platba"),
                    tx2.column4.map(_.value).contains("3558"),
                    tx2.column5.map(_.value).contains("40000000")
                )
        }
    )