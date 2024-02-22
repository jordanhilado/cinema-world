ThisBuild / version := "0.1.0-SNAPSHOT"

ThisBuild / scalaVersion := "2.13.13"

libraryDependencies ++= Seq(
  "com.typesafe.akka" %% "akka-http" % "10.2.6",
  "com.typesafe.akka" %% "akka-stream" % "2.6.16",
  "com.typesafe.akka" %% "akka-http-spray-json" % "10.2.6"
)

lazy val root = (project in file("."))
  .settings(
    name := "cinema-world"
  )
