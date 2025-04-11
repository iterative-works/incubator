package works.iterative.incubator.ynab.web.view

import scalatags.Text.TypedTag
import works.iterative.incubator.ynab.domain.model.YnabAccountMapping

/**
 * Interface for YNAB account mapping views
 */
trait YnabAccountMappingViews:
    /**
     * View for the account mapping list page
     * 
     * @param mappings List of account mappings
     * @param sourceAccountMap Map of source account IDs to names
     * @param ynabAccountMap Map of YNAB account IDs to names
     * @return HTML for the account mapping list page
     */
    def accountMappingList(
        mappings: List[YnabAccountMapping],
        sourceAccountMap: Map[Long, String],
        ynabAccountMap: Map[String, String]
    ): TypedTag[String]
    
    /**
     * View for the account mapping form (create/edit)
     * 
     * @param mapping Optional existing mapping (if editing)
     * @param sourceAccounts Available source accounts for mapping
     * @param ynabAccounts Available YNAB accounts for mapping
     * @return HTML for the account mapping form
     */
    def accountMappingForm(
        mapping: Option[YnabAccountMapping],
        sourceAccounts: Seq[(Long, String)],
        ynabAccounts: Seq[(String, String)]
    ): TypedTag[String]
    
    /**
     * View for when a mapping is not found
     * 
     * @param sourceAccountId The source account ID that wasn't found
     * @return HTML for the not found page
     */
    def mappingNotFound(sourceAccountId: Long): TypedTag[String]
end YnabAccountMappingViews