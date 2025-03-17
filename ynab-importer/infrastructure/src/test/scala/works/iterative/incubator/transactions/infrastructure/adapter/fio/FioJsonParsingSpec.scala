package works.iterative.incubator.transactions.infrastructure.adapter.fio

import zio.*
import zio.test.*
import zio.test.Assertion.*
import zio.json.*
import FioCodecs.given

object FioJsonParsingSpec extends ZIOSpecDefault:
    def spec = suite("Fio JSON Parsing")(
        test("should parse the example JSON correctly") {
            for
                jsonStr <- ZIO.attemptBlocking {
                    val stream = getClass.getClassLoader.getResourceAsStream("example_fio.json")
                    val content = scala.io.Source.fromInputStream(stream).mkString
                    stream.close()
                    content
                }

                // Parse the JSON
                result <- ZIO.fromEither(jsonStr.fromJson[FioResponse])
                    .mapError(err => new RuntimeException(s"JSON parsing failed: $err"))
            yield
            // Verify key properties
            assertTrue(
                result.accountStatement.info.accountId == "2200000001",
                result.accountStatement.info.currency == "CZK",
                result.accountStatement.transactionList.transaction.size == 2,
                result.accountStatement.transactionList.transaction.head.column22.exists(
                    _.value == 26962199069L
                ),
                result.accountStatement.transactionList.transaction.head.column1.exists(
                    _.value == 50000.0
                ),
                result.accountStatement.transactionList.transaction(1).column1.exists(
                    _.value == -3000.0
                )
            )
        }
    )
end FioJsonParsingSpec
