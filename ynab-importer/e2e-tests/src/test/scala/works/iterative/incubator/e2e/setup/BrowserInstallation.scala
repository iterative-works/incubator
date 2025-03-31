package works.iterative.incubator.e2e.setup

import zio.*
import java.nio.file.{Files, Path, Paths}
import java.util.concurrent.TimeUnit

/** Utilities for browser installation and verification
  */
object BrowserInstallation:

    /** Class for browser installation result
      *
      * @param installed
      *   Whether the browser is installed
      * @param installOutput
      *   Output from the installation process
      */
    case class InstallationResult(installed: Boolean, installOutput: String = "")

    /** Check if Playwright browsers are installed
      *
      * This checks if the browser binaries are present in the expected directory
      *
      * @return
      *   True if all required browsers are installed
      */
    def areBrowsersInstalled: Task[Boolean] = ZIO.attempt {
        // Check for browser installations
        val homeDir = java.lang.System.getProperty("user.home")
        val playwrightPath = Paths.get(homeDir, ".cache", "ms-playwright")

        // Check for at least one browser installation
        val browsersExist = Files.exists(playwrightPath) && {
            val browserDirs = Files.list(playwrightPath).toArray
            browserDirs.exists(p =>
                Files.isDirectory(p.asInstanceOf[Path]) &&
                    (p.asInstanceOf[Path].getFileName.toString.contains("chromium") ||
                        p.asInstanceOf[Path].getFileName.toString.contains("firefox") ||
                        p.asInstanceOf[Path].getFileName.toString.contains("webkit"))
            )
        }

        browsersExist
    }.catchAll(_ => ZIO.succeed(false))

    /** Install Playwright browsers if not already installed
      *
      * @return
      *   Installation result with status and output
      */
    def installBrowsersIfNeeded: Task[InstallationResult] =
        for
            installed <- areBrowsersInstalled
            result <- if !installed then installBrowsers else ZIO.succeed(InstallationResult(true))
        yield result

    /** Install Playwright browsers
      *
      * Runs the Playwright CLI installation command
      *
      * @return
      *   Installation result with status and output
      */
    private def installBrowsers: Task[InstallationResult] =
        // Run the Playwright CLI installation command
        ZIO.logInfo("Installing Playwright browsers...") *> ZIO.attemptBlocking {

            val command = Array("npx", "playwright", "install")
            val process = new ProcessBuilder(command*)
                .redirectErrorStream(true)
                .start()

            // Wait for the installation to complete with a reasonable timeout
            val completed = process.waitFor(10, TimeUnit.MINUTES)

            if completed then
                // Read the output
                val output = scala.io.Source.fromInputStream(process.getInputStream).mkString
                val exitCode = process.exitValue()

                if exitCode == 0 then
                    InstallationResult(true, output)
                else
                    InstallationResult(
                        false,
                        s"Installation failed with exit code $exitCode: $output"
                    )
                end if
            else
                process.destroyForcibly()
                InstallationResult(false, "Installation timed out after 10 minutes")
            end if
        }

    /** Validate that Playwright browsers are installed and available
      *
      * @return
      *   ZIO that fails if browsers are not installed
      */
    def validateBrowserInstallation: Task[Unit] =
        for
            installed <- areBrowsersInstalled
            _ <-
                if installed
                then ZIO.logInfo("Playwright browsers are already installed.")
                else
                    ZIO.logWarning(
                        "Playwright browsers are not installed, attempting installation..."
                    ) *>
                        installBrowsers.flatMap { result =>
                            if result.installed
                            then ZIO.logInfo("Playwright browsers installed successfully.")
                            else
                                ZIO.fail(new RuntimeException(
                                    s"Failed to install Playwright browsers: ${result.installOutput}"
                                ))
                        }
        yield ()

end BrowserInstallation
