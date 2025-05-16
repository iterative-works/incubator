import com.typesafe.sbt.packager.docker._
import NativePackagerHelper._

ThisBuild / version := "0.1.0-SNAPSHOT"

ThisBuild / scalaVersion := scala3Version

lazy val iwSupportVersion = "0.1.10-SNAPSHOT"

lazy val support = new IWDeps.Support(iwSupportVersion)

ThisBuild / resolvers ++= Seq(
    "IW releases" at "https://dig.iterative.works/maven/releases",
    "IW snapshots" at "https://dig.iterative.works/maven/snapshots"
)

// Common dependencies
lazy val commonDependencies = IWDeps.useZIO() ++ Seq(
    IWDeps.zioLogging,
    support.libs.core
)

// Web UI common module
lazy val webUi = (project in file("web-ui"))
    .settings(name := "web-ui")
    .enablePlugins(IWScalaProjectPlugin)
    .settings(
        commonDependencies,
        IWDeps.scalatags,
        support.libs.ui,
        support.supportLib("ui-scalatags"),
        support.supportLib("forms-http")
    )

// Core module (shared across all contexts)
lazy val core = (project in file("bounded-contexts/core"))
    .settings(name := "core")
    .enablePlugins(IWScalaProjectPlugin)
    .settings(commonDependencies)
    .settings(IWDeps.magnum)

// Bounded Contexts

// Budget module (shared kernel)
lazy val budget = (project in file("bounded-contexts/budget"))
    .settings(name := "budget")
    .enablePlugins(IWScalaProjectPlugin)
    .settings(commonDependencies, support.supportLib("server-http"), support.supportLib("sqldb"))
    .dependsOn(core, webUi)

lazy val `budget-it` = (project in file("bounded-contexts/budget/it")).settings(
    name := "budget-it"
).settings(IWDeps.useZIO(), IWDeps.logbackClassic, support.supportLib("sqldb-testing")).dependsOn(
    budget
)

// Component preview module
lazy val preview = (project in file("preview"))
    .settings(name := "preview")
    .enablePlugins(JavaAppPackaging, IWScalaProjectPlugin)
    .settings(
        commonDependencies,
        IWDeps.http4sBlazeServer,
        IWDeps.logbackClassic,
        support.supportLib("server-http"),
        reStart / mainClass := Some("works.iterative.incubator.ui.preview.PreviewServerMain"),
        reStart / javaOptions += "-DLOG_LEVEL=DEBUG",
        reStart / envVars ++= Map(
            "BASEURI" -> "/",
            "VITE_BASE" -> "http://localhost:5173/",
            "VITE_DISTPATH" -> "./target/vite",
            "BLAZE_PORT" -> "8090"
        ),
        reStart / aggregate := false
    )
    .dependsOn(budget, webUi)

lazy val root = (project in file("."))
    .settings(name := "iw-incubator")
    // Auto activates for all projects, but make sure we have required dependencies
    .enablePlugins(VitePlugin, JavaServerAppPackaging, DockerPlugin, IWScalaProjectPlugin)
    .settings(
        commonDependencies,
        IWDeps.http4sBlazeServer,
        IWDeps.logbackClassic,
        support.supportLib("server-http"),
        reStart / javaOptions += "-DLOG_LEVEL=DEBUG",
        reStart / envVars ++= Map(
            "BASEURI" -> "/",
            "VITE_BASE" -> "http://localhost:5173/",
            "VITE_DISTPATH" -> "./target/vite",
            "PG_URL" -> "jdbc:postgresql://storage:5432/incubator",
            "PG_USERNAME" -> "incubator"
        ),
        reStart / aggregate := false,
        Test / fork := true,
        Test / envVars ++= Map(
            "BASEURI" -> "/",
            "VITE_BASE" -> "http://localhost:5173/",
            "VITE_DISTPATH" -> "./target/vite",
            "PG_URL" -> "jdbc:postgresql://storage:5432/incubator",
            "PG_USERNAME" -> "incubator"
        )
    )
    .settings(
        // Docker configuration
        Docker / packageName := "iw-incubator",
        Docker / publish / aggregate := false,
        Docker / publishLocal / aggregate := false,
        dockerBaseImage := "eclipse-temurin:21-jre",
        dockerExposedPorts := Seq(8080),
        dockerUpdateLatest := true,
        dockerEnvVars := Map(
            "BLAZE_HOST" -> "0.0.0.0",
            "BLAZE_PORT" -> "8080",
            "VITE_FILE" -> "/opt/docker/vite/.vite/manifest.json",
            "VITE_DISTPATH" -> "/opt/docker/vite",
            "JAVA_OPTS" -> "-Xmx512m -Xms256m",
            "LOG_LEVEL" -> "INFO"
        ),
        // Add healthcheck
        dockerCommands += Cmd(
            "HEALTHCHECK",
            "--interval=30s",
            "--timeout=5s",
            "--start-period=30s",
            "--retries=3",
            "CMD",
            "curl",
            "-f",
            "http://localhost:8080/health",
            "||",
            "exit",
            "1"
        ),
        Docker / mappings ++= directory(viteBuild.value).map { case (f, p) =>
            f -> s"/opt/docker/$p"
        }
    )
    .dependsOn(budget, webUi)
    .aggregate(webUi, core, budget, preview)
