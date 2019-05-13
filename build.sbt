name := "ddata-tools"

organization := "com.evolutiongaming"

homepage := Some(new URL("http://github.com/evolution-gaming/ddata-tools"))

startYear := Some(2018)

organizationName := "Evolution Gaming"

organizationHomepage := Some(url("http://evolutiongaming.com"))

bintrayOrganization := Some("evolutiongaming")

scalaVersion := crossScalaVersions.value.last

crossScalaVersions := Seq("2.12.8")

scalacOptions in (Compile,doc) ++= Seq("-no-link-warnings")

resolvers += Resolver.bintrayRepo("evolutiongaming", "maven")

libraryDependencies ++= Seq(
  "com.typesafe.akka"   %% "akka-distributed-data" % "2.5.22",
  "com.typesafe.akka"   %% "akka-testkit"          % "2.5.22" % Test,
  "com.evolutiongaming" %% "executor-tools"        % "1.0.1",
  "org.scalatest"       %% "scalatest"             % "3.0.7"  % Test)

licenses := Seq(("MIT", url("https://opensource.org/licenses/MIT")))

releaseCrossBuild := true