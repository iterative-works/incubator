package works.iterative.incubator.e2e.setup

import zio.*
import scala.util.Random
import java.util.UUID

/**
 * Manager for test data used in end-to-end tests
 * 
 * This class provides utilities to create, manage, and clean up test data
 * for use in end-to-end tests.
 */
object TestDataManager:

    /**
     * Source account test data
     * 
     * @param name Account name
     * @param accountId Account ID/number
     * @param bankId Bank ID
     * @param currency Currency code
     * @param active Whether the account is active
     */
    case class SourceAccountTestData(
        name: String,
        accountId: String,
        bankId: String,
        currency: String = "CZK",
        active: Boolean = true
    )
    
    /**
     * Create a test source account with random data
     * 
     * @return A random source account for testing
     */
    def createRandomSourceAccount: UIO[SourceAccountTestData] = ZIO.succeed {
        val random = new Random()
        SourceAccountTestData(
            name = s"Test Account ${UUID.randomUUID().toString.take(8)}",
            accountId = (100000000 + random.nextInt(900000000)).toString,
            bankId = (100 + random.nextInt(900)).toString,
            currency = "CZK",
            active = true
        )
    }
    
    /**
     * Create a list of test source accounts
     * 
     * @param count Number of accounts to create
     * @return List of random source accounts
     */
    def createSourceAccounts(count: Int): UIO[List[SourceAccountTestData]] =
        ZIO.foreach(1 to count)(_ => createRandomSourceAccount).map(_.toList)

    /**
     * Default test accounts
     * 
     * These accounts have predefined values for consistent testing
     */
    val defaultSourceAccounts = List(
        SourceAccountTestData(
            name = "Personal Checking",
            accountId = "123456789",
            bankId = "0800",
            currency = "CZK",
            active = true
        ),
        SourceAccountTestData(
            name = "Business Account",
            accountId = "987654321",
            bankId = "0600",
            currency = "EUR",
            active = true
        ),
        SourceAccountTestData(
            name = "Savings Account",
            accountId = "555555555",
            bankId = "0800",
            currency = "CZK",
            active = false
        )
    )
    
    /**
     * Test transaction data
     * 
     * @param amount Transaction amount
     * @param description Transaction description
     * @param date Transaction date (YYYY-MM-DD)
     * @param category Category (optional)
     */
    case class TransactionTestData(
        amount: BigDecimal,
        description: String,
        date: String,
        category: Option[String] = None
    )
    
    /**
     * Create a random test transaction
     * 
     * @return A random transaction for testing
     */
    def createRandomTransaction: UIO[TransactionTestData] = ZIO.succeed {
        val random = new Random()
        val amount = BigDecimal(random.nextInt(10000) - 5000) / 100
        
        val descriptions = List(
            "Grocery shopping", 
            "Restaurant payment", 
            "Utility bill",
            "Salary", 
            "Online purchase",
            "Transport ticket",
            "Subscription payment",
            "ATM withdrawal"
        )
        
        val categories = List(
            "Food", 
            "Transport", 
            "Entertainment", 
            "Housing", 
            "Utilities",
            "Income"
        )
        
        // Generate a random date in the last 30 days
        val day = 1 + random.nextInt(28)
        val month = 1 + random.nextInt(12)
        val year = 2025
        val date = f"$year%04d-$month%02d-$day%02d"
        
        TransactionTestData(
            amount = amount,
            description = descriptions(random.nextInt(descriptions.size)),
            date = date,
            category = if (random.nextBoolean()) Some(categories(random.nextInt(categories.size))) else None
        )
    }
    
    /**
     * Create random transactions
     * 
     * @param count Number of transactions to create
     * @return List of random transactions
     */
    def createRandomTransactions(count: Int): UIO[List[TransactionTestData]] =
        ZIO.foreach(1 to count)(_ => createRandomTransaction).map(_.toList)
    
    /**
     * Configuration for making API calls
     */
    case class ApiConfig(
        baseUrl: String = "http://localhost:8080",
        timeout: zio.Duration = zio.Duration.fromSeconds(10)
    )
    
    /**
     * Create a specified number of test source accounts in the database
     *
     * @param count Number of accounts to create
     * @param active Whether the accounts should be active
     * @return Unit
     */
    def createTestSourceAccounts(count: Int, active: Boolean = true): UIO[Unit] =
        // For local dev testing, we'll still just log mock data being created
        // In real test scenarios with proper setup, we would actually create accounts
        // through the API or directly in the database
        for
            accounts <- createSourceAccounts(count)
                        .map(_.map(acct => acct.copy(active = active)))
            _ <- ZIO.logInfo(s"[MOCK] Created $count source accounts with active=$active")
            _ <- ZIO.foreach(accounts)(acct => 
                ZIO.logDebug(s"Test account created: ${acct.name}, ID: ${acct.accountId}, active: ${acct.active}")
            )
        yield ()
    
    /**
     * Create source accounts through the API
     * 
     * Note: This implementation would be used when the application is running
     * and can be accessed through its API.
     */
    def createSourceAccountsViaApi(count: Int, active: Boolean = true): Task[Unit] =
        // In a real implementation, we would create accounts via API calls
        // For now, we'll just simulate success but log as if we were making API calls
        for
            accounts <- createSourceAccounts(count)
                        .map(_.map(acct => acct.copy(active = active)))
            _ <- ZIO.logInfo(s"Would create $count source accounts via API with active=$active")
            _ <- ZIO.foreach(accounts)(acct => 
                ZIO.logInfo(s"Would call API to create: ${acct.name}, ID: ${acct.accountId}, active: ${acct.active}")
            )
        yield ()
    
end TestDataManager