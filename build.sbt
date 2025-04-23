import com.typesafe.sbt.packager.docker._

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
    .settings(commonDependencies)
    .dependsOn(core)

// Transaction Management Context
lazy val transactions = (project in file("bounded-contexts/transactions"))
    .settings(name := "transactions")
    .enablePlugins(IWScalaProjectPlugin)
    .settings(
        commonDependencies,
        scalacOptions ++= Seq("-Xmax-inlines", "64"),
        IWDeps.zioJson,
        IWDeps.magnumZIO,
        IWDeps.magnumPG,
        IWDeps.http4sBlazeServer,
        IWDeps.scalatags,
        IWDeps.chimney,
        support.supportLib("sqldb")
    )
    .dependsOn(core, budget, webUi)

lazy val `transactions-it` = (project in file("bounded-contexts/transactions/it")).settings(
    name := "transactions-it"
).settings(IWDeps.useZIO(), IWDeps.logbackClassic, support.supportLib("sqldb-testing")).dependsOn(
    transactions
)

// YNAB Integration Context
lazy val ynab = (project in file("bounded-contexts/ynab"))
    .settings(name := "ynab")
    .settings(
        commonDependencies,
        IWDeps.zioJson,
        IWDeps.sttpClient4Core,
        IWDeps.sttpClient4Lib("zio")
    )
    .dependsOn(core, budget, transactions)

lazy val `ynab-it` = (project in file("bounded-contexts/ynab/it"))
    .settings(name := "ynab-it")
    .settings(IWDeps.useZIO())
    .dependsOn(ynab)

// Fio Bank Context
lazy val fio = (project in file("bounded-contexts/fio"))
    .settings(name := "fio")
    .enablePlugins(IWScalaProjectPlugin)
    .settings(
        commonDependencies,
        IWDeps.zioJson,
        IWDeps.sttpClient4Core,
        IWDeps.sttpClient4Lib("zio"),
        IWDeps.magnumZIO,
        IWDeps.magnumPG,
        IWDeps.chimney,
        IWDeps.logbackClassic
    )
    .dependsOn(core, budget, transactions)

lazy val `fio-it` = (project in file("bounded-contexts/fio/it"))
    .settings(name := "fio-it")
    .settings(IWDeps.useZIO())
    .dependsOn(fio)

// AI Categorization Context (Skeleton)
lazy val categorization = (project in file("bounded-contexts/categorization"))
    .settings(name := "categorization")
    .enablePlugins(IWScalaProjectPlugin)
    .settings(commonDependencies)
    .settings(
        libraryDependencies += "com.softwaremill.sttp.openai" %% "zio" % "0.3.4"
    )
    .dependsOn(core, budget, transactions)

lazy val `categorization-it` = (project in file("bounded-contexts/categorization/it"))
    .settings(name := "categorization-it")
    .settings(IWDeps.useZIO())
    .dependsOn(categorization)

// Import Module Context
lazy val imports = (project in file("bounded-contexts/imports"))
    .settings(name := "imports")
    .enablePlugins(IWScalaProjectPlugin)
    .settings(
        commonDependencies,
        IWDeps.zioJson,
        IWDeps.http4sBlazeServer,
        IWDeps.scalatags
    )
    .dependsOn(core, budget, transactions, fio)

lazy val `imports-it` = (project in file("bounded-contexts/imports/it"))
    .settings(name := "imports-it")
    .settings(IWDeps.useZIO())
    .dependsOn(imports)

// User Management Context (Skeleton)
lazy val auth = (project in file("bounded-contexts/auth"))
    .settings(name := "auth")
    .enablePlugins(IWScalaProjectPlugin)
    .settings(
        commonDependencies,
        IWDeps.zioJson
    )
    .dependsOn(core)

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
        Test / fork := true
    )
    .settings(
        // Docker configuration
        Docker / packageName := "iw-incubator",
        dockerBaseImage := "eclipse-temurin:21-jre-alpine",
        dockerExposedPorts := Seq(8080),
        dockerUpdateLatest := true,
        dockerEnvVars := Map(
            "BLAZE_HOST" -> "0.0.0.0",
            "BLAZE_PORT" -> "8080",
            "BASEURI" -> "/",
            "VITE_FILE" -> "/opt/docker/vite/manifest.json",
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
        )
    )
    .dependsOn(transactions, ynab, fio, imports, categorization)
    .aggregate(webUi, core, budget, transactions, ynab, fio, categorization, auth, imports)
