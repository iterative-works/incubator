package works.iterative.incubator.ynab.domain.model

/**
 * Mapping between source accounts and YNAB accounts
 *
 * This entity maintains the relationship between source accounts in the transactions context
 * and YNAB accounts, acting as an anti-corruption layer between the two bounded contexts.
 *
 * @param sourceAccountId
 *   ID of the source account in the transactions context
 * @param ynabAccountId
 *   ID of the corresponding account in YNAB
 * @param active
 *   Whether this mapping is active
 *
 * Classification: Domain Entity (ACL)
 */
case class YnabAccountMapping(
    sourceAccountId: Long,
    ynabAccountId: String,
    active: Boolean = true
)

/**
 * Command to create a new YNAB account mapping
 *
 * @param sourceAccountId
 *   ID of the source account in the transactions context
 * @param ynabAccountId
 *   ID of the corresponding account in YNAB
 * @param active
 *   Whether this mapping is active
 *
 * Classification: Domain Value Object (Command)
 */
case class CreateYnabAccountMapping(
    sourceAccountId: Long,
    ynabAccountId: String,
    active: Boolean = true
)