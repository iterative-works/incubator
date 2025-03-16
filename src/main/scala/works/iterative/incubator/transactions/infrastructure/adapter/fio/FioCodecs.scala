package works.iterative.incubator.transactions
package infrastructure
package adapter.fio

import zio.json.*

object FioCodecs:
    // Generic decoder for FioTransactionValue
    given stringValueDecoder: JsonDecoder[FioTransactionValue[String]] =
        DeriveJsonDecoder.gen[FioTransactionValue[String]]
    given longValueDecoder: JsonDecoder[FioTransactionValue[Long]] =
        DeriveJsonDecoder.gen[FioTransactionValue[Long]]
    given doubleValueDecoder: JsonDecoder[FioTransactionValue[Double]] =
        DeriveJsonDecoder.gen[FioTransactionValue[Double]]

    given transactionDecoder: JsonDecoder[FioTransaction] = DeriveJsonDecoder.gen[FioTransaction]
    given transactionListDecoder: JsonDecoder[FioTransactionList] =
        DeriveJsonDecoder.gen[FioTransactionList]
    given statementInfoDecoder: JsonDecoder[FioStatementInfo] =
        DeriveJsonDecoder.gen[FioStatementInfo]
    given accountStatementDecoder: JsonDecoder[FioAccountStatement] =
        DeriveJsonDecoder.gen[FioAccountStatement]
    given responseDecoder: JsonDecoder[FioResponse] = DeriveJsonDecoder.gen[FioResponse]
end FioCodecs
