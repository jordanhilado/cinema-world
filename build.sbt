import com.typesafe.sbt.packager.docker._

val circeVersion = "0.14.1"

lazy val root = (project in file("."))
  .enablePlugins(PlayScala)
  .enablePlugins(JavaAppPackaging)
  .enablePlugins(DockerPlugin)
  //.enablePlugins(PlayNettyServer).disablePlugins(PlayPekkoHttpServer) // uncomment to use the Netty backend
  .settings(
    name := """cinema-world""",
    version := "1.0-SNAPSHOT",
    // dockerBaseImage := "openjdk:23-slim",
    dockerBaseImage := "openjdk:23-ea-8-jdk-slim",
    dockerCommands ++= Seq(
      ExecCmd("RUN", "mkdir", "-p", "/opt/docker/logs/"),
      ExecCmd("RUN", "chmod", "+w", "-R", "/opt/docker/logs/")
    ),
    crossScalaVersions := Seq("2.13.12", "3.3.1"),
    scalaVersion := crossScalaVersions.value.head,
    libraryDependencies ++= Seq(
      guice,
      "com.h2database" % "h2" % "2.2.224",
      "org.scalatestplus.play" %% "scalatestplus-play" % "7.0.1" % Test,
    ),
    libraryDependencies ++= Seq(
      "io.circe" %% "circe-core",
      "io.circe" %% "circe-generic",
      "io.circe" %% "circe-parser",
    ).map(_ % circeVersion),
    libraryDependencies ++= Seq(
      "com.typesafe.slick" %% "slick" % "3.4.1",
      "org.slf4j" % "slf4j-nop" % "1.7.26",
      "com.typesafe.slick" %% "slick-hikaricp" % "3.4.1",
      "org.postgresql" % "postgresql" % "42.2.5"
    ),
    scalacOptions ++= Seq(
      "-feature",
      "-Werror"
    ),
    // Needed for ssl-config to create self signed certificated under Java 17
    Test / javaOptions ++= List("--add-exports=java.base/sun.security.x509=ALL-UNNAMED"),
    Universal / javaOptions ++= Seq(
      "-Dpidfile.path=/dev/null"
    )
  )
