package works.iterative.incubator.e2e.setup

import zio.*
import com.microsoft.playwright.Page
import com.typesafe.config.ConfigFactory
import java.nio.file.{Files, Path, Paths}
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter

/** Support for capturing screenshots during tests
  */
trait ScreenshotSupport:

    /** Configuration for screenshot capture
      *
      * @param directory
      *   Directory to save screenshots
      * @param captureOnFailure
      *   Whether to capture screenshots on test failures
      */
    case class ScreenshotConfig(
        directory: String = "target/e2e-screenshots",
        captureOnFailure: Boolean = true
    )

    // Load screenshot configuration from application.conf
    private val loadConfig: Task[ScreenshotConfig] = ZIO.attempt {
        val config = ConfigFactory.load().getConfig("e2e-tests.screenshots")
        ScreenshotConfig(
            directory = config.getString("directory"),
            captureOnFailure = config.getBoolean("captureOnFailure")
        )
    }.catchAll(error =>
        // Log warning about using default config
        ZIO.logWarning(s"Failed to load screenshot config, using defaults: ${error.getMessage}") *>
            ZIO.succeed(ScreenshotConfig())
    )

    /** Capture a screenshot
      *
      * @param page
      *   The Playwright page to capture
      * @param testName
      *   Name of the test (used in filename)
      * @param suffix
      *   Optional suffix to append to the filename
      * @return
      *   Path to the screenshot file
      */
    def captureScreenshot(
        page: Page,
        testName: String,
        suffix: String = ""
    ): ZIO[Any, Throwable, Path] =
        for
            config <- loadConfig
            timestamp = LocalDateTime.now.format(DateTimeFormatter.ofPattern("yyyyMMdd-HHmmss"))
            // Clean testName to use as filename (replace spaces and special chars with underscores)
            cleanTestName = testName.replaceAll("[^a-zA-Z0-9]", "_")
            // Create directory if it doesn't exist
            directory = Paths.get(config.directory)
            _ <- ZIO.attempt(Files.createDirectories(directory))
            // Create filename with timestamp
            suffix2 = if suffix.isEmpty then "" else s"-$suffix"
            filename = s"${cleanTestName}${suffix2}-${timestamp}.png"
            path = directory.resolve(filename)
            // Take screenshot
            _ <- ZIO.logInfo(s"Capturing screenshot to $path")
            _ <- ZIO.attempt(page.screenshot(new Page.ScreenshotOptions().setPath(path)))
        yield path

    /** Helper to take a screenshot on test failure
      *
      * @param page
      *   The Playwright page to capture
      * @param testName
      *   Name of the test for the filename
      * @return
      *   A ZIO that will run on test failure
      */
    def screenshotOnFailure(
        page: Page,
        testName: String
    ): ZIO[Any, Nothing, Unit] =
        loadConfig.foldZIO(
            _ => ZIO.unit, // On config loading error, skip screenshot
            config =>
                if config.captureOnFailure then
                    captureScreenshot(page, testName, "failure")
                        .foldZIO(
                            err =>
                                ZIO.logError(
                                    s"Failed to capture failure screenshot: ${err.getMessage}"
                                ),
                            path => ZIO.logInfo(s"Captured failure screenshot at $path")
                        )
                else
                    ZIO.unit
        )

end ScreenshotSupport
