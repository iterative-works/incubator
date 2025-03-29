package works.iterative.incubator.e2e.tests

import zio.*
import zio.test.*
import zio.test.Assertion.*
import works.iterative.incubator.e2e.PlaywrightSupport
import com.microsoft.playwright.{ElementHandle, Page}

/**
 * E2E tests for Source Account Management feature
 * 
 * Based on the Gherkin feature file: source_account_management.feature
 */
object SourceAccountManagementSpec extends ZIOSpecDefault with PlaywrightSupport {

  // Custom config for running tests (non-headless for easier debugging)
  val testConfig = defaultConfig.copy(
    headless = false,  // Set to true in CI environment
    slowMo = 100,      // Slow down execution to see what's happening
    timeout = 60000,   // Increase timeout for more complex operations
  )

  // Test implementation
  override def spec = suite("Source Account Management")(
    
    /**
     * Scenario: Create a new source account
     * 
     * When I navigate to the source accounts page
     * And I click on "Add New Account"
     * Then I should see the account creation form
     * When I fill in the account details
     * And I click on "Create Account"
     * Then a new source account should be created with a unique ID
     * And I should be redirected to the account details page
     */
    test("Creating a new source account") {
      withPlaywright(
        for {
          // When I navigate to the source accounts page
          _ <- navigateTo("/source-accounts")
          
          // And I click on "Add New Account"
          _ <- click("a[href='/source-accounts/new']")
          
          // Then I should see the account creation form
          formExists <- elementExists("form[action='/source-accounts']")
          
          // When I fill in the account details
          _ <- fill("#name", "Test Checking Account")
          _ <- fill("#accountId", "123456789")
          _ <- fill("#bankId", "0800")
          _ <- selectOption("#currency", "CZK")
          _ <- checkOption("#active")
          
          // And I click on "Create Account"
          _ <- click("button[type='submit']")
          
          // Wait for navigation to complete - we expect to be redirected to the account details page
          _ <- waitForUrlContains("/source-accounts/")
          _ <- waitForLoadComplete
          
          // Then I should be redirected to the account details page
          currentUrl <- ZIO.serviceWith[Page](_.url())
          isRedirectedToDetails <- ZIO.succeed(currentUrl.contains("/source-accounts/"))
          
          // And I should see the account name in the details
          accountName <- getText("h1")
        } yield {
          assertTrue(
            formExists,
            isRedirectedToDetails,
            accountName.contains("Test Checking Account")
          )
        },
        testConfig
      )
    },
    
    /**
     * Scenario: View list of source accounts
     * 
     * When I navigate to the source accounts page
     * Then I should see a list of all source accounts
     * And I should see details including account name, bank ID, and status
     */
    test("Viewing the list of source accounts") {
      withPlaywright(
        for {
          // When I navigate to the source accounts page
          _ <- navigateTo("/source-accounts")
          
          // Then I should see a list of all source accounts
          tableExists <- elementExists("table")
          headerRow <- findElement("table > thead > tr")
          headerTexts <- ZIO.attempt(headerRow.textContent())
          
          // And I should see details including account name, bank ID, and status
          hasNameColumn = headerTexts.contains("Name")
          hasBankIdColumn = headerTexts.contains("Bank ID")
          hasStatusColumn = headerTexts.contains("Status")
          
          // Check if any accounts are displayed
          accountRowsResult <- findElements("table > tbody > tr")
            .foldZIO(
              _ => ZIO.succeed(List.empty[ElementHandle]),
              rows => ZIO.succeed(rows)
            )
          hasAccounts = accountRowsResult.nonEmpty
        } yield {
          assertTrue(
            tableExists,
            hasNameColumn,
            hasBankIdColumn,
            hasStatusColumn,
            hasAccounts
          )
        },
        testConfig
      )
    },
    
    /**
     * Scenario: Filter source accounts by status
     * 
     * When I navigate to the source accounts page
     * And I select "Active" from the status filter
     * Then I should see only active source accounts
     */
    test("Filtering source accounts by status") {
      withPlaywright(
        for {
          // When I navigate to the source accounts page
          _ <- navigateTo("/source-accounts")
          
          // And I select "Active" from the status filter
          _ <- selectOption("#status-filter", "active")
          
          // Wait for the page to reload
          _ <- ZIO.sleep(1.second)
          
          // Verify that the "active" option is selected
          page <- ZIO.service[Page]
          selectedOption <- ZIO.attempt(
            page.evalOnSelector("#status-filter", "el => el.value").toString
          )
          
          // Check if all displayed accounts have "Active" status
          statusBadges <- findElements(".badge")
          statusTexts <- ZIO.foreach(statusBadges)(badge => ZIO.attempt(badge.textContent()))
          allStatusesActive = statusTexts.forall(status => status.contains("Active"))
        } yield {
          assertTrue(
            selectedOption == "active",
            allStatusesActive
          )
        },
        testConfig
      )
    }
  )
}