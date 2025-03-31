package works.iterative.incubator.e2e.tests

import zio.*
import zio.test.*
import works.iterative.incubator.e2e.PlaywrightSupport

/** Simple test for checking the framework compilation
  */
object SourceAccountManagementSpec extends ZIOSpecDefault with PlaywrightSupport:

    // Simple test implementation
    override def spec = suite("Source Account Management")(
        test("Verify e2e framework compiles") {
            withPlaywright(
                for
                    // We only do the minimum to verify the framework compiles
                    exists <- ZIO.succeed(true)
                yield assertTrue(exists)
            )
        }
    )
end SourceAccountManagementSpec
