package works.iterative.incubator.e2e.setup

// No imports needed here

/**
 * Helper class to install Playwright browsers
 * 
 * Run this before executing the tests for the first time
 */
object InstallBrowsers {
  def main(args: Array[String]): Unit = {
    // Install the browsers
    val installArgs = Array("install")
    
    // For specific browsers, you can add them as arguments
    // val installArgs = Array("install", "chromium", "firefox", "webkit")
    
    println("Installing Playwright browsers...")
    com.microsoft.playwright.CLI.main(installArgs)
    println("Browsers installed successfully!")
  }
}