import Dependencies._

name := "ddata-tools"

organization := "com.evolutiongaming"

homepage := Some(new URL("http://github.com/evolution-gaming/ddata-tools"))

startYear := Some(2018)

organizationName := "Evolution"

organizationHomepage := Some(url("http://evolution.com"))

scalaVersion := crossScalaVersions.value.head

crossScalaVersions := Seq("2.13.11", "2.12.18")

Compile / doc / scalacOptions ++= Seq("-no-link-warnings")

publishTo := Some(Resolver.evolutionReleases)

libraryDependencies ++= Seq(
  Akka.actor,
  Akka.cluster,
  Akka.`distributed-data`,
  Akka.testkit % Test,
  Cats.core,
  Cats.effect,
  `executor-tools`,
  `cats-helper`,
  smetrics,
  scalatest % Test)

licenses := Seq(("MIT", url("https://opensource.org/licenses/MIT")))

releaseCrossBuild := true

scalacOptsFailOnWarn := Some(false)