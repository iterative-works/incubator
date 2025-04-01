package works.iterative.incubator.e2e.setup

import zio.*
import org.testcontainers.containers.{Network, PostgreSQLContainer, GenericContainer}
import org.testcontainers.containers.wait.strategy.Wait
import java.time.Duration
import com.typesafe.config.ConfigFactory
import scala.jdk.CollectionConverters.*
import java.util.Properties

/**
  * TestContainersSupport provides ZIO wrappers for working with TestContainers
  * 
  * It manages Docker containers for:
  * - PostgreSQL database
  * - The YNAB Importer application
  */
object TestContainersSupport:
    // Configuration for the application container
    case class AppContainerConfig(
        imageName: String = "ynab-importer:0.1.0", 
        port: Int = 8080,
        healthPath: String = "/health",
        startupTimeoutSeconds: Int = 120
    )

    // Configuration for the PostgreSQL container
    case class PostgresContainerConfig(
        imageName: String = "postgres:16.2-alpine", 
        databaseName: String = "ynab-test",
        username: String = "ynab-test",
        password: String = "ynab-test",
        port: Int = 5432
    )

    // Create a PostgreSQL container configured for our tests
    private def createPostgresContainer(config: PostgresContainerConfig): PostgreSQLContainer[Nothing] =
        val container = new PostgreSQLContainer[Nothing](config.imageName)
        container.withDatabaseName(config.databaseName)
        container.withUsername(config.username)
        container.withPassword(config.password)
        container.withExposedPorts(config.port)
        container

    // Create an application container configured to use the PostgreSQL container
    private def createAppContainer(
        config: AppContainerConfig, 
        pgContainer: PostgreSQLContainer[Nothing],
        network: Network
    ): GenericContainer[Nothing] =
        val container = new GenericContainer[Nothing](config.imageName)
        container.withExposedPorts(config.port)
        container.withNetwork(network)
        container.withEnv("PG_URL", s"jdbc:postgresql://postgres:5432/${pgContainer.getDatabaseName}")
        container.withEnv("PG_USERNAME", pgContainer.getUsername)
        container.withEnv("PG_PASSWORD", pgContainer.getPassword)
        container.withEnv("LOG_LEVEL", "DEBUG")
        container.waitingFor(
            Wait.forHttp(config.healthPath)
                .forPort(config.port)
                .forStatusCode(200)
                .withStartupTimeout(Duration.ofSeconds(config.startupTimeoutSeconds))
        )
        
        container

    // Run a test with TestContainers environment
    def withTestContainers[A](test: ZIO[Any, Throwable, A]): ZIO[Any, Throwable, A] =
        // For now, we'll just provide a simple implementation that mocks the container environment
        // The full implementation would start actual containers
        val mockTestEnv = ZIO.succeed {
            // Mock settings that would normally come from containers
            val properties = new Properties()
            properties.setProperty("baseUrl", "http://localhost:8080")
            
            // Return the mocked config
            ConfigFactory.parseProperties(properties)
        }

        // Run the test with our mocked environment
        ZIO.scoped {
            for
                _ <- ZIO.logInfo("Starting mock test containers environment")
                config <- mockTestEnv
                result <- test
                _ <- ZIO.logInfo("Test containers environment cleaned up")
            yield result
        }

    // The full implementation of withTestContainers (to be implemented later)
    def withRealTestContainers[A](test: ZIO[Any, Throwable, A]): ZIO[Any, Throwable, A] =
        ZIO.scoped {
            for
                _ <- ZIO.logInfo("Starting test containers")
                
                // Create shared network for containers
                network <- ZIO.acquireRelease(
                    ZIO.attempt(Network.newNetwork())
                )(network => ZIO.attempt(network.close()).orDie)
                
                // Configure and start PostgreSQL container
                pgConfig = PostgresContainerConfig()
                pgContainer <- ZIO.acquireRelease(
                    ZIO.attempt {
                        val container = createPostgresContainer(pgConfig)
                        container.withNetwork(network)
                        container.withNetworkAliases("postgres")
                        container.start()
                        container
                    }
                )(container => ZIO.attempt(container.stop()).orDie)
                
                // Wait a bit for postgres to be fully ready
                _ <- ZIO.sleep(5.seconds)
                
                // Configure and start application container
                appConfig = AppContainerConfig()
                appContainer <- ZIO.acquireRelease(
                    ZIO.attempt {
                        val container = createAppContainer(appConfig, pgContainer, network)
                        container.start()
                        container
                    }
                )(container => ZIO.attempt(container.stop()).orDie)
                
                // Map host port for the application
                mappedPort = appContainer.getMappedPort(appConfig.port)
                baseUrl = s"http://localhost:$mappedPort"
                
                // Create a config with the container details
                config = ConfigFactory.parseMap(Map(
                    "baseUrl" -> baseUrl,
                    "database.url" -> pgContainer.getJdbcUrl,
                    "database.user" -> pgContainer.getUsername,
                    "database.password" -> pgContainer.getPassword
                ).asJava)
                
                // Run the test with the container configuration
                result <- test
            yield result
        }
end TestContainersSupport