package works.iterative.incubator.e2e

import com.microsoft.playwright.*
import com.microsoft.playwright.options.LoadState
import zio.*
import zio.logging.*
import works.iterative.incubator.e2e.setup.{BrowserInstallation, ScreenshotSupport, TestContainersSupport}
import scala.util.Try

/**
 * Base trait for Playwright test support
 */
trait PlaywrightSupport extends ScreenshotSupport {

  /**
   * Configuration for Playwright tests
   */
  case class PlaywrightConfig(
      baseUrl: String,
      headless: Boolean,
      slowMo: Int,
      timeout: Int,
      browserType: String,
      viewportWidth: Int,
      viewportHeight: Int
  )

  /**
   * Default configuration values
   */
  val defaultConfig = PlaywrightConfig(
    baseUrl = "http://localhost:8080",
    headless = true,
    slowMo = 0,
    timeout = 30000,
    browserType = "chromium",
    viewportWidth = 1280,
    viewportHeight = 720
  )

  /**
   * ZIO layer for Playwright
   */
  val playwrightLayer: ZLayer[Any, Throwable, Playwright] =
    ZLayer.scoped {
      // Validate browser installation before creating Playwright instance
      BrowserInstallation.validateBrowserInstallation *>
      ZIO.acquireRelease(
        ZIO.attempt(Playwright.create())
      )(playwright => ZIO.succeed(playwright.close()))
    }

  /**
   * ZIO layer for Browser
   */
  val browserLayer: ZLayer[PlaywrightConfig & Playwright, Throwable, Browser] =
    ZLayer.scoped {
      for {
        config <- ZIO.service[PlaywrightConfig]
        playwright <- ZIO.service[Playwright]
        browserType <- ZIO.attempt {
          config.browserType match {
            case "chromium" => playwright.chromium()
            case "firefox" => playwright.firefox()
            case "webkit" => playwright.webkit()
            case _ => playwright.chromium()
          }
        }
        browser <- ZIO.acquireRelease(
          ZIO.attempt(
            browserType.launch(
              new BrowserType.LaunchOptions()
                .setHeadless(config.headless)
                .setSlowMo(config.slowMo)
                .setTimeout(config.timeout)
            )
          )
        )(browser => ZIO.succeed(browser.close()))
      } yield browser
    }

  /**
   * ZIO layer for Page
   */
  val pageLayer: ZLayer[Browser & PlaywrightConfig, Throwable, Page] =
    ZLayer.scoped {
      for {
        browser <- ZIO.service[Browser]
        config <- ZIO.service[PlaywrightConfig]
        context <- ZIO.acquireRelease(
          ZIO.attempt(
            browser.newContext(
              new Browser.NewContextOptions()
                .setViewportSize(config.viewportWidth, config.viewportHeight)
            )
          )
        )(context => ZIO.succeed(context.close()))
        page <- ZIO.acquireRelease(
          ZIO.attempt(context.newPage())
        )(page => ZIO.succeed(page.close()))
      } yield page
    }

  /**
   * Helper method to create a Playwright test with auto-screenshots on failure
   * 
   * This version uses a user-provided configuration.
   */
  def withPlaywright[E, A](
      test: RIO[Page & PlaywrightConfig, A],
      config: PlaywrightConfig = defaultConfig
  ): ZIO[Any, Throwable, A] = {
    // Get test name from the call site
    val testName = Thread.currentThread().getStackTrace()
      .find(_.getClassName.contains("$anonfun$"))
      .map(_.getMethodName)
      .getOrElse("unknown-test")
    
    // Create a new layer with the config
    val configLayer = ZLayer.succeed(config)
    
    // We need to add console logging
    val loggingLayer = Runtime.removeDefaultLoggers >>> consoleLogger()
    
    // The full environment for running tests
    val testEnv = ZLayer.make[Page & PlaywrightConfig](
      configLayer,
      playwrightLayer,
      browserLayer,
      pageLayer,
      loggingLayer
    )
    
    // Run the test with screenshot on failure, dealing with potential ScreenshotOnFailure errors
    test.provideLayer(testEnv)
      .tapError { _ =>
        // On error, take a screenshot
        for
          page <- ZIO.service[Page].provideLayer(testEnv)
          _ <- screenshotOnFailure(page, testName)
        yield ()
      }.catchAllCause(cause => 
        // Log the error properly
        ZIO.logErrorCause(s"Test failed: $testName", cause) *> 
        ZIO.fail(cause.squash)
      ).provide(loggingLayer)
  }

  /**
   * Helper method to create a Playwright test with TestContainers
   * 
   * This version starts TestContainers for both the database and application
   * and automatically configures Playwright based on the containers.
   */
  def withTestContainers[E, A](
      test: RIO[Page & PlaywrightConfig, A]
  ): ZIO[Any, Throwable, A] = {
    // For our current simplified implementation, just delegate to TestContainersSupport
    TestContainersSupport.withTestContainers {
      // And use the withPlaywright method to actually run the test
      withPlaywright(test)
    }
  }

  /**
   * Helper method to navigate to a URL
   */
  def navigateTo(path: String): ZIO[Page & PlaywrightConfig, Throwable, Response] =
    for {
      page <- ZIO.service[Page]
      config <- ZIO.service[PlaywrightConfig]
      url = s"${config.baseUrl}$path"
      _ <- ZIO.logInfo(s"Navigating to $url")
      response <- ZIO.attempt(page.navigate(url))
    } yield response

  /**
   * Helper method to find an element
   */
  def findElement(selector: String): ZIO[Page, Throwable, ElementHandle] =
    for {
      page <- ZIO.service[Page]
      element <- ZIO.attempt(page.querySelector(selector))
      result <- ZIO.fromOption(Option(element))
        .mapError(_ => new NoSuchElementException(s"Could not find element with selector: $selector"))
    } yield result

  /**
   * Helper method to find elements
   */
  def findElements(selector: String): ZIO[Page, Throwable, List[ElementHandle]] =
    for {
      page <- ZIO.service[Page]
      elements <- ZIO.attempt(page.querySelectorAll(selector))
      // Convert Java collection to Scala List
      list <- ZIO.attempt {
        import scala.jdk.CollectionConverters.*
        elements.asScala.toList
      }
    } yield list

  /**
   * Helper method to click an element
   */
  def click(selector: String): ZIO[Page, Throwable, Unit] =
    for {
      page <- ZIO.service[Page]
      _ <- ZIO.logInfo(s"Clicking element with selector: $selector")
      _ <- ZIO.attempt(page.click(selector))
    } yield ()

  /**
   * Helper method to fill a form field
   */
  def fill(selector: String, value: String): ZIO[Page, Throwable, Unit] =
    for {
      page <- ZIO.service[Page]
      _ <- ZIO.logInfo(s"Filling element $selector with value: $value")
      _ <- ZIO.attempt(page.fill(selector, value))
    } yield ()

  /**
   * Helper method to wait for navigation
   * 
   * Note: Using waitForURL instead of the deprecated waitForNavigation
   * This waits for the URL to contain the specified substring
   */
  def waitForUrlContains(urlSubstring: String): ZIO[Page, Throwable, Unit] =
    for {
      page <- ZIO.service[Page]
      _ <- ZIO.attempt {
        // Create a regular expression pattern that matches URLs containing the substring
        val pattern = s".*${urlSubstring}.*"
        // Wait for the URL to match the pattern
        page.waitForURL(pattern)
      }
    } yield ()
    
  /**
   * Helper method to wait for navigation to complete
   * 
   * This just waits for the page's network to become idle
   */
  def waitForLoadComplete: ZIO[Page, Throwable, Unit] =
    for {
      page <- ZIO.service[Page]
      _ <- ZIO.attempt {
        // Wait for the page to load completely
        page.waitForLoadState(LoadState.NETWORKIDLE)
      }
    } yield ()

  /**
   * Helper method to get element text
   */
  def getText(selector: String): ZIO[Page, Throwable, String] =
    for {
      element <- findElement(selector)
      text <- ZIO.attempt(element.innerText())
    } yield text

  /**
   * Helper method to check if an element exists
   */
  def elementExists(selector: String): ZIO[Page, Nothing, Boolean] =
    findElement(selector).as(true).orElse(ZIO.succeed(false))

  /**
   * Helper method to select an option from a dropdown
   */
  def selectOption(selector: String, value: String): ZIO[Page, Throwable, Unit] =
    for {
      page <- ZIO.service[Page]
      _ <- ZIO.logInfo(s"Selecting option $value in dropdown $selector")
      _ <- ZIO.attempt(page.selectOption(selector, value))
    } yield ()

  /**
   * Helper method to check a checkbox
   */
  def checkOption(selector: String, checked: Boolean = true): ZIO[Page, Throwable, Unit] =
    for {
      page <- ZIO.service[Page]
      _ <- ZIO.logInfo(s"Setting checkbox $selector to $checked")
      _ <- if (checked) ZIO.attempt(page.check(selector)) else ZIO.attempt(page.uncheck(selector))
    } yield ()

  /**
   * Helper method to wait for a selector to appear
   */
  def waitForSelector(selector: String): ZIO[Page, Throwable, ElementHandle] =
    for {
      page <- ZIO.service[Page]
      _ <- ZIO.logInfo(s"Waiting for selector: $selector")
      element <- ZIO.attempt(page.waitForSelector(selector))
    } yield element

  /**
   * Take a screenshot at the current point in the test
   * 
   * @param name Optional name to identify this screenshot
   * @return Path to the screenshot file
   */
  def takeScreenshot(name: String = ""): ZIO[Page, Throwable, Unit] =
    for {
      page <- ZIO.service[Page]
      // Get test name from the call site
      testName = Try(
        Thread.currentThread().getStackTrace()
          .find(_.getClassName.contains("$anonfun$"))
          .map(_.getMethodName)
          .getOrElse("unknown-test")
      ).getOrElse("manual-screenshot")
      
      suffix = if name.isEmpty then "" else name
      _ <- captureScreenshot(page, testName, suffix)
    } yield ()

  // Import for Scala collection conversions
  import scala.jdk.CollectionConverters.*
}