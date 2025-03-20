ThisBuild / version := "0.1.0-SNAPSHOT"

ThisBuild / scalaVersion := scala3Version

lazy val iwSupportVersion = "0.1.9-SNAPSHOT"

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

// YNAB Importer modules
lazy val ynabImporterCore = (project in file("ynab-importer/core"))
    .settings(name := "ynab-importer-core")
    .enablePlugins(IWScalaProjectPlugin)
    .settings(commonDependencies)

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
            "org.postgresql" % "postgresql" % "42.7.5",
            "com.zaxxer" % "HikariCP" % "6.2.1"
        )
    )
    .dependsOn(ynabImporterCore)

lazy val ynabImporterInfrastructureIT = (project in file("ynab-importer/infrastructure-it"))
    .settings(name := "ynab-importer-infrastructure-it")
    .enablePlugins(IWScalaProjectPlugin)
    .settings(commonDependencies)
    .settings(publish / skip := true)
    .settings(
        libraryDependencies += "com.dimafeng" %% "testcontainers-scala-postgresql" % "0.43.0" % Test
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
    .dependsOn(ynabImporterApp, webUi)

lazy val ynabImporter = (project in file("ynab-importer"))
    .settings(name := "ynab-importer")
    .enablePlugins(IWScalaProjectPlugin)
    .aggregate(
        ynabImporterCore,
        ynabImporterInfrastructure,
        ynabImporterApp,
        ynabImporterWeb
    )

lazy val root = (project in file("."))
    .settings(name := "iw-incubator")
    // Auto activates for all projects, but make sure we have required dependencies
    .enablePlugins(VitePlugin, JavaServerAppPackaging, IWScalaProjectPlugin)
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
    .dependsOn(ynabImporterInfrastructure, ynabImporterWeb)
    .aggregate(webUi, ynabImporter)
