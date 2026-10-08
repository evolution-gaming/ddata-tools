import Dependencies.*

name := "ddata-tools"

organization := "com.evolutiongaming"

homepage := Some(uri("https://github.com/evolution-gaming/ddata-tools"))

startYear := Some(2018)

organizationName := "Evolution"

organizationHomepage := Some(uri("https://evolution.com"))

scalaVersion := crossScalaVersions.value.head

crossScalaVersions := Seq("2.13.18", "3.3.8")

Compile / doc / scalacOptions ++= Seq("-release:17", "-deprecation", "-no-link-warnings")

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
  scalatest % Test,
)

licenses := Seq(("MIT", uri("https://opensource.org/licenses/MIT")))

scalacOptsFailOnWarn := Some(false)

versionPolicyIntention := Compatibility.BinaryCompatible

addCommandAlias("check", "+all versionPolicyCheck scalafmtCheckRepo Compile/doc")
addCommandAlias("fmt", "scalafmtRepo")
addCommandAlias("build", "+all compile testFull")
