package works.iterative.incubator.fio.infrastructure.client

import zio.test.*
import zio.json.*
import zio.nio.file.{Files, Path}
import zio.test.Assertion.*
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
                jsonString <- Files.readAllBytes(Path(exampleJsonFile)).map(new String(_))
                parsed <- ZIO.fromEither(jsonString.fromJson[FioResponse])
            yield
                assert(parsed)(isA[FioResponse]) &&
                assert(parsed.accountStatement.info.accountId)(equalTo("2200000001")) &&
                assert(parsed.accountStatement.info.bankId)(equalTo("2010")) &&
                assert(parsed.accountStatement.info.currency)(equalTo("CZK")) &&
                assert(parsed.accountStatement.info.iban)(equalTo("CZ5420100000002200000001")) &&
                assert(parsed.accountStatement.info.bic)(equalTo("FIOBCZPPXXX")) &&
                assert(parsed.accountStatement.transactionList.transaction.size)(equalTo(2))
        },

        test("should correctly parse transaction values") {
            for
                jsonString <- Files.readAllBytes(Path(exampleJsonFile)).map(new String(_))
                parsed <- ZIO.fromEither(jsonString.fromJson[FioResponse])
                tx = parsed.accountStatement.transactionList.transaction.head
            yield
                assert(tx.column22.map(_.value))(isSome(equalTo(26962199069L))) &&
                assert(tx.column0.map(_.value))(isSome(equalTo("2025-03-14+0100"))) &&
                assert(tx.column1.map(_.value))(isSome(equalTo(50000.0)))
        },

        test("should correctly handle null fields") {
            for
                jsonString <- Files.readAllBytes(Path(exampleJsonFile)).map(new String(_))
                parsed <- ZIO.fromEither(jsonString.fromJson[FioResponse])
                tx = parsed.accountStatement.transactionList.transaction.head
            yield
                assert(tx.column4)(isNone) &&
                assert(tx.column5)(isNone) &&
                assert(tx.column6)(isNone)
        },

        test("should handle transaction fields for both transactions") {
            for
                jsonString <- Files.readAllBytes(Path(exampleJsonFile)).map(new String(_))
                parsed <- ZIO.fromEither(jsonString.fromJson[FioResponse])
                tx1 = parsed.accountStatement.transactionList.transaction(0)
                tx2 = parsed.accountStatement.transactionList.transaction(1)
            yield
                // First transaction assertions
                assert(tx1.column1.map(_.value))(isSome(equalTo(50000.0))) &&
                assert(tx1.column8.map(_.value))(isSome(equalTo("Příjem převodem uvnitř banky"))) &&
                
                // Second transaction assertions
                assert(tx2.column1.map(_.value))(isSome(equalTo(-3000.0))) &&
                assert(tx2.column8.map(_.value))(isSome(equalTo("Bezhotovostní platba"))) &&
                assert(tx2.column4.map(_.value))(isSome(equalTo("3558"))) &&
                assert(tx2.column5.map(_.value))(isSome(equalTo("40000000")))
        }
    )