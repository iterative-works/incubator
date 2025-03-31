package works.iterative.incubator.e2e.setup

import zio.*
import zio.Console.*

/** Helper class to install Playwright browsers
  *
  * Run this before executing the tests for the first time
  */
object InstallBrowsers extends ZIOAppDefault:

    /** A ZIO application that installs the required browsers
      *
      * This is more robust than the previous implementation and provides better error reporting.
      */
    override def run: ZIO[Any, Any, Any] =
        for
            _ <- printLine("Checking for Playwright browsers...")
            installed <- BrowserInstallation.areBrowsersInstalled
            _ <- if installed then
                printLine("Playwright browsers are already installed. Skipping installation.")
            else
                printLine("Playwright browsers not found, installing...") *>
                    BrowserInstallation.installBrowsersIfNeeded.flatMap { result =>
                        if result.installed then
                            printLine("Browsers installed successfully!")
                        else
                            printLine(s"Failed to install browsers: ${result.installOutput}")
                    }
        yield ()

    /** Alternative method for running directly
      */
    def installBrowsers(): Unit =
        // Use ZIO runtime to run the application
        val _ = Unsafe.unsafely:
            Runtime.default.unsafe.run(run).getOrThrowFiberFailure()
    end installBrowsers
end InstallBrowsers
