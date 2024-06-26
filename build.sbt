ThisBuild / version := "0.1.0-SNAPSHOT"

ThisBuild / scalaVersion := "2.13.14"

lazy val root = (project in file("."))
  .settings(
    name := "fp-tax-calculator"
  )

val kindProjectorVersion = "0.13.3"
val http4sVersion = "1.0.0-M41"
val circeVersion = "0.14.7"
val log4catsVersion = "2.7.0"
val mongo4catsVersion = "0.7.6"
val pureconfigVersion = "0.17.7"
val scalaLoggingVersion = "3.9.5"
val slf4jVersion = "1.7.36"

val scalaTestVersion = "3.2.19"

addCompilerPlugin(
  "org.typelevel" % "kind-projector" % kindProjectorVersion cross CrossVersion.full
)

libraryDependencies ++= Seq(
  "org.http4s" %% "http4s-ember-server" % http4sVersion,
  "org.http4s" %% "http4s-circe" % http4sVersion,
  "org.http4s" %% "http4s-dsl" % http4sVersion,
  "io.circe" %% "circe-generic" % circeVersion,
  "org.typelevel" %% "log4cats-slf4j" % log4catsVersion,
  "io.github.kirill5k" %% "mongo4cats-core" % mongo4catsVersion,
  "io.github.kirill5k" %% "mongo4cats-circe" % mongo4catsVersion,
  "com.github.pureconfig" %% "pureconfig" % pureconfigVersion,
  "com.typesafe.scala-logging" %% "scala-logging" % scalaLoggingVersion,
  "org.slf4j" % "slf4j-simple" % slf4jVersion,


"org.scalatest" %% "scalatest" % scalaTestVersion % Test,
  "io.github.kirill5k" %% "mongo4cats-embedded" % mongo4catsVersion % Test
)
