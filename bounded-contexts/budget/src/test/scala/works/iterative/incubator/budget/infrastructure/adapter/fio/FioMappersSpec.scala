package works.iterative.incubator.budget.infrastructure.adapter.fio

import works.iterative.incubator.budget.domain.model.*
import works.iterative.incubator.budget.infrastructure.adapter.fio.FioModels.*
import zio.test.*
import zio.test.Assertion.*
import java.time.LocalDate
import java.util.Currency

object FioMappersSpec extends ZIOSpecDefault:
    def spec = suite("FioMappers")(
        suite("parseDate")(
            test("should correctly parse a valid date") {
                val result = FioMappers.parseDate("2025-03-14+0100")
                assert(result)(isRight(equalTo(LocalDate.of(2025, 3, 14))))
            },
            test("should return Left for an invalid date") {
                val result = FioMappers.parseDate("invalid-date")
                assert(result.isLeft)(isTrue)
            }
        ),
        suite("combineReferences")(
            test("should combine multiple references with appropriate formatting") {
                val result = FioMappers.combineReferences(
                    Some("123"),
                    Some("456789"),
                    Some("789")
                )
                assert(result)(isSome(equalTo("KS:123, VS:456789, SS:789")))
            },
            test("should handle missing reference values") {
                val result = FioMappers.combineReferences(None, Some("456789"), None)
                assert(result)(isSome(equalTo("VS:456789")))
            },
            test("should return None when all references are missing") {
                val result = FioMappers.combineReferences(None, None, None)
                assert(result)(isNone)
            }
        ),
        suite("createDescription")(
            test("should combine transaction type and comment") {
                val result = FioMappers.createDescription(
                    Some("Payment"),
                    Some("Invoice #123")
                )
                assert(result)(equalTo("Payment - Invoice #123"))
            },
            test("should handle missing comment") {
                val result = FioMappers.createDescription(Some("Payment"), None)
                assert(result)(equalTo("Payment"))
            },
            test("should handle missing transaction type") {
                val result = FioMappers.createDescription(None, Some("Invoice #123"))
                assert(result)(equalTo("Invoice #123"))
            },
            test("should handle both missing") {
                val result = FioMappers.createDescription(None, None)
                assert(result)(equalTo("Unknown transaction"))
            }
        ),
        suite("mapToDomainTransaction")(
            test("should map a complete Fio transaction to domain Transaction") {
                // Create a sample Fio transaction
                val fioTransaction = FioTransaction(
                    column0 = Some(Column(0, "Datum", "2025-03-14+0100")),
                    column1 = Some(Column(1, "Objem", BigDecimal("1000.50"))),
                    column2 = Some(Column(2, "Protiúčet", "2800000002")),
                    column3 = Some(Column(3, "Kód banky", "2010")),
                    column4 = Some(Column(4, "KS", "123")),
                    column5 = Some(Column(5, "VS", "456789")),
                    column6 = Some(Column(6, "SS", "789")),
                    column7 = Some(Column(7, "Uživatelská identifikace", "User ID")),
                    column8 = Some(Column(8, "Typ", "Příjem převodem uvnitř banky")),
                    column10 = Some(Column(10, "Název protiúčtu", "Novák, Jan")),
                    column12 = Some(Column(12, "Název banky", "Fio banka, a.s.")),
                    column14 = Some(Column(14, "Měna", "CZK")),
                    column17 = Some(Column(17, "ID pokynu", BigDecimal("12345"))),
                    column22 = Some(Column(22, "ID pohybu", BigDecimal("26962199069"))),
                    column25 = Some(Column(25, "Komentář", "Test payment")),
                    column26 = Some(Column(26, "BIC", "FIOBCZPPXXX"))
                )

                // Create test account and batch IDs
                val accountId = AccountId("fio", "1234567890")
                val importBatchId = ImportBatchId("fio-1234567890", 1)

                // Map the transaction
                val result = FioMappers.mapToDomainTransaction(
                    fioTransaction,
                    accountId,
                    importBatchId
                )

                // Assertions
                assert(result.isRight)(isTrue) &&
                result.toOption.map { transaction =>
                    assert(transaction.date)(equalTo(LocalDate.of(2025, 3, 14))) &&
                    assert(transaction.amount.amount)(equalTo(BigDecimal("1000.50"))) &&
                    assert(transaction.amount.currency)(equalTo(Currency.getInstance("CZK"))) &&
                    assert(transaction.counterAccount)(isSome(equalTo("2800000002/2010"))) &&
                    assert(transaction.counterparty)(isSome(equalTo("Novák, Jan"))) &&
                    assert(transaction.reference)(isSome(containsString("KS:123"))) &&
                    assert(transaction.reference)(isSome(containsString("VS:456789"))) &&
                    assert(transaction.reference)(isSome(containsString("SS:789"))) &&
                    assert(transaction.description)(
                        equalTo("Příjem převodem uvnitř banky - Test payment")
                    ) &&
                    assert(transaction.importBatchId)(equalTo(importBatchId)) &&
                    assert(transaction.status)(equalTo(TransactionStatus.Imported))
                }.getOrElse(assert(true)(isFalse))
            },
            test("should handle missing optional fields gracefully") {
                // Create a minimal Fio transaction with only required fields
                val fioTransaction = FioTransaction(
                    column0 = Some(Column(0, "Datum", "2025-03-14+0100")),
                    column1 = Some(Column(1, "Objem", BigDecimal("1000.50"))),
                    column14 = Some(Column(14, "Měna", "CZK")),
                    column22 = Some(Column(22, "ID pohybu", BigDecimal("26962199069"))),
                    column8 = Some(Column(8, "Typ", "Příjem převodem uvnitř banky")),
                    column2 = None,
                    column3 = None,
                    column4 = None,
                    column5 = None,
                    column6 = None,
                    column7 = None,
                    column10 = None,
                    column12 = None,
                    column17 = None,
                    column25 = None,
                    column26 = None
                )

                // Create test account and batch IDs
                val accountId = AccountId("fio", "1234567890")
                val importBatchId = ImportBatchId("fio-1234567890", 1)

                // Map the transaction
                val result = FioMappers.mapToDomainTransaction(
                    fioTransaction,
                    accountId,
                    importBatchId
                )

                // Assertions
                assert(result.isRight)(isTrue) &&
                result.toOption.map { transaction =>
                    assert(transaction.date)(equalTo(LocalDate.of(2025, 3, 14))) &&
                    assert(transaction.amount.amount)(equalTo(BigDecimal("1000.50"))) &&
                    assert(transaction.amount.currency)(equalTo(Currency.getInstance("CZK"))) &&
                    assert(transaction.counterAccount)(isNone) &&
                    assert(transaction.counterparty)(isNone) &&
                    assert(transaction.reference)(isNone) &&
                    assert(transaction.description)(equalTo("Příjem převodem uvnitř banky")) &&
                    assert(transaction.importBatchId)(equalTo(importBatchId)) &&
                    assert(transaction.status)(equalTo(TransactionStatus.Imported))
                }.getOrElse(assert(true)(isFalse))
            },
            test("should return Left for missing required fields") {
                // Create a Fio transaction missing required fields
                val fioTransaction = FioTransaction(
                    column0 = None, // Missing date
                    column1 = Some(Column(1, "Objem", BigDecimal("1000.50"))),
                    column14 = Some(Column(14, "Měna", "CZK")),
                    column22 = Some(Column(22, "ID pohybu", BigDecimal("26962199069"))),
                    column2 = None,
                    column3 = None,
                    column4 = None,
                    column5 = None,
                    column6 = None,
                    column7 = None,
                    column8 = None,
                    column10 = None,
                    column12 = None,
                    column17 = None,
                    column25 = None,
                    column26 = None
                )

                // Create test account and batch IDs
                val accountId = AccountId("fio", "1234567890")
                val importBatchId = ImportBatchId("fio-1234567890", 1)

                // Map the transaction
                val result = FioMappers.mapToDomainTransaction(
                    fioTransaction,
                    accountId,
                    importBatchId
                )

                // Assertion
                assert(result.isLeft)(isTrue) &&
                assert(result.left.toOption.get)(containsString("date"))
            }
        )
    )
end FioMappersSpec
