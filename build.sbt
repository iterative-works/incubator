ThisBuild / version := "0.1.0-SNAPSHOT"

ThisBuild / scalaVersion := scala3Version

lazy val iwSupportVersion = "0.1.9-SNAPSHOT"

lazy val root = (project in file("."))
    .settings(name := "iw-incubator")
    // Auto activates for all projects, but make sure we have required dependencies
    .enablePlugins(VitePlugin, JavaServerAppPackaging, IWScalaProjectPlugin)
    .settings(
        scalacOptions ++= Seq("-Xmax-inlines", "64"),
        IWDeps.useZIO(),
        IWDeps.zioLogging,
        IWDeps.http4sBlazeServer,
        IWDeps.logbackClassic,
        IWDeps.scalatags,
        IWDeps.chimney,
        IWDeps.magnumZIO,
        IWDeps.magnumPG,
        libraryDependencies ++= Seq(
            "works.iterative.support" %% "iw-support-server-http" % iwSupportVersion,
            "works.iterative.support" %% "iw-support-ui" % iwSupportVersion,
            "works.iterative.support" %% "iw-support-ui-scalatags" % iwSupportVersion,
            "works.iterative.support" %% "iw-support-forms-http" % iwSupportVersion,
            "org.postgresql" % "postgresql" % "42.7.5",
            "com.zaxxer" % "HikariCP" % "6.2.1"
        ),
        reStart / javaOptions += "-DLOG_LEVEL=DEBUG",
        reStart / envVars ++= Map(
            "BASEURI" -> "/",
            "VITE_BASE" -> "http://localhost:5173/",
            "VITE_DISTPATH" -> "./target/vite",
            "PG_URL" -> "jdbc:postgresql://storage:5432/incubator",
            "PG_USERNAME" -> "incubator"
        )
    )
