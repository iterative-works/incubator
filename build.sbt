import com.typesafe.sbt.packager.docker._

ThisBuild / version := "0.1.0-SNAPSHOT"

ThisBuild / scalaVersion := scala3Version

lazy val iwSupportVersion = "0.1.10-SNAPSHOT"

ThisBuild / resolvers ++= Seq(
    "IW releases" at "https://dig.iterative.works/maven/releases",
    "IW snapshots" at "https://dig.iterative.works/maven/snapshots"
)

// Common dependencies
lazy val commonDependencies = IWDeps.useZIO() ++ Seq(
    IWDeps.zioLogging,
    libraryDependencies += "works.iterative.support" %% "iw-support-core" % iwSupportVersion
)

// Web UI common module
lazy val webUi = (project in file("web-ui"))
    .settings(name := "web-ui")
    .enablePlugins(IWScalaProjectPlugin)
    .settings(
        commonDependencies,
        IWDeps.scalatags,
        libraryDependencies ++= Seq(
            "works.iterative.support" %% "iw-support-ui" % iwSupportVersion,
            "works.iterative.support" %% "iw-support-ui-scalatags" % iwSupportVersion,
            "works.iterative.support" %% "iw-support-forms-http" % iwSupportVersion
        )
    )

// Core module (shared across all contexts)
lazy val core = (project in file("bounded-contexts/core"))
    .settings(name := "core")
    .enablePlugins(IWScalaProjectPlugin)
    .settings(commonDependencies)

// Bounded Contexts 

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
        libraryDependencies ++= Seq(
            "org.flywaydb" % "flyway-core" % "11.4.0",
            "org.flywaydb" % "flyway-database-postgresql" % "11.4.0",
            "org.postgresql" % "postgresql" % "42.7.5",
            "com.zaxxer" % "HikariCP" % "6.2.1"
        )
    )
    .dependsOn(core, webUi)

// YNAB Integration Context
lazy val ynab = (project in file("bounded-contexts/ynab"))
    .settings(name := "ynab")
    .enablePlugins(IWScalaProjectPlugin)
    .settings(
        commonDependencies,
        IWDeps.zioJson,
        IWDeps.sttpClient3Core,
        IWDeps.sttpClient3Lib("async-http-client-backend-zio")
    )
    .dependsOn(core, transactions)

// Fio Bank Context
lazy val fio = (project in file("bounded-contexts/fio"))
    .settings(name := "fio")
    .enablePlugins(IWScalaProjectPlugin)
    .settings(
        commonDependencies,
        IWDeps.zioJson,
        IWDeps.sttpClient3Core,
        IWDeps.sttpClient3Lib("async-http-client-backend-zio")
    )
    .dependsOn(core, transactions)

// AI Categorization Context (Skeleton)
lazy val categorization = (project in file("bounded-contexts/categorization"))
    .settings(name := "categorization")
    .enablePlugins(IWScalaProjectPlugin)
    .settings(commonDependencies)
    .dependsOn(core, transactions)

// User Management Context (Skeleton)
lazy val auth = (project in file("bounded-contexts/auth"))
    .settings(name := "auth")
    .enablePlugins(IWScalaProjectPlugin)
    .settings(
        commonDependencies,
        IWDeps.zioJson
    )
    .dependsOn(core)

// Maintain existing module structure for backward compatibility during migration
// YNAB Importer modules
lazy val ynabImporterCore = (project in file("ynab-importer/core"))
    .settings(name := "ynab-importer-core")
    .enablePlugins(IWScalaProjectPlugin)
    .settings(commonDependencies)
    .dependsOn(core, transactions, ynab, fio, categorization, auth)

lazy val ynabImporterInfrastructure = (project in file("ynab-importer/infrastructure"))
    .settings(name := "ynab-importer-infrastructure")
    .enablePlugins(IWScalaProjectPlugin)
    .settings(
        commonDependencies,
        scalacOptions ++= Seq("-Xmax-inlines", "64"),
        IWDeps.zioJson,
        IWDeps.magnumZIO,
        IWDeps.magnumPG,
        IWDeps.chimney,
        IWDeps.sttpClient3Core,
        IWDeps.sttpClient3Lib("async-http-client-backend-zio"),
        libraryDependencies ++= Seq(
            "org.flywaydb" % "flyway-core" % "11.4.0",
            "org.flywaydb" % "flyway-database-postgresql" % "11.4.0",
            "org.postgresql" % "postgresql" % "42.7.5",
            "com.zaxxer" % "HikariCP" % "6.2.1"
        )
    )
    .dependsOn(ynabImporterCore, transactions, ynab, fio)

lazy val ynabImporterInfrastructureIT = (project in file("ynab-importer/infrastructure-it"))
    .settings(name := "ynab-importer-infrastructure-it")
    .enablePlugins(IWScalaProjectPlugin)
    .settings(commonDependencies)
    .settings(publish / skip := true)
    .settings(
        IWDeps.logbackClassic,
        libraryDependencies ++= Seq(
            // Use latest testcontainers (testcontainers-scala pulls in older version)
            "org.testcontainers" % "testcontainers" % "1.20.6" % Test,
            "org.testcontainers" % "postgresql" % "1.20.6" % Test,
            "com.dimafeng" %% "testcontainers-scala-postgresql" % "0.43.0" % Test
        )
    )
    .dependsOn(ynabImporterCore, ynabImporterInfrastructure)

lazy val ynabImporterApp = (project in file("ynab-importer/app"))
    .settings(name := "ynab-importer-app")
    .enablePlugins(IWScalaProjectPlugin)
    .settings(commonDependencies)
    .dependsOn(ynabImporterCore, ynabImporterInfrastructure)

lazy val ynabImporterWeb = (project in file("ynab-importer/web"))
    .settings(name := "ynab-importer-web")
    .enablePlugins(IWScalaProjectPlugin)
    .settings(
        commonDependencies,
        IWDeps.http4sBlazeServer,
        IWDeps.scalatags,
        libraryDependencies ++= Seq(
            "works.iterative.support" %% "iw-support-ui" % iwSupportVersion,
            "works.iterative.support" %% "iw-support-ui-scalatags" % iwSupportVersion,
            "works.iterative.support" %% "iw-support-forms-http" % iwSupportVersion
        )
    )
    .dependsOn(ynabImporterApp, webUi, transactions)

lazy val ynabImporterE2ETests = (project in file("ynab-importer/e2e-tests"))
    .settings(name := "ynab-importer-e2e-tests")
    .enablePlugins(IWScalaProjectPlugin)
    .settings(
        commonDependencies,
        // Dependencies for the e2e tests module
        IWDeps.zioLib("test-junit", IWVersions.zio, Test),
        IWDeps.logbackClassic,
        libraryDependencies ++= Seq(
            // Playwright
            "com.microsoft.playwright" % "playwright" % "1.51.0" % Test,

            // TestContainers
            "org.testcontainers" % "testcontainers" % "1.20.6",
            "org.testcontainers" % "postgresql" % "1.20.6",
            "com.dimafeng" %% "testcontainers-scala-postgresql" % "0.43.0",

            // Config
            "com.typesafe" % "config" % "1.4.3" % Test
        )
    )
    .settings(publish / skip := true)
    .dependsOn(ynabImporterCore, ynabImporterWeb)

lazy val ynabImporter = (project in file("ynab-importer"))
    .settings(name := "ynab-importer")
    .enablePlugins(IWScalaProjectPlugin)
    .aggregate(
        core,
        transactions,
        ynab,
        fio,
        categorization,
        auth,
        ynabImporterCore,
        ynabImporterInfrastructure,
        ynabImporterApp,
        ynabImporterWeb,
        ynabImporterE2ETests
    )

lazy val root = (project in file("."))
    .settings(name := "iw-incubator")
    // Auto activates for all projects, but make sure we have required dependencies
    .enablePlugins(VitePlugin, JavaServerAppPackaging, DockerPlugin, IWScalaProjectPlugin)
    .settings(
        commonDependencies,
        IWDeps.http4sBlazeServer,
        IWDeps.logbackClassic,
        libraryDependencies ++= Seq(
            "works.iterative.support" %% "iw-support-server-http" % iwSupportVersion
        ),
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
    .dependsOn(transactions, ynab, fio, ynabImporterInfrastructure, ynabImporterWeb)
    .aggregate(webUi, core, transactions, ynab, fio, categorization, auth, ynabImporter)
