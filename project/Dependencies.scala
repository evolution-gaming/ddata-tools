import sbt._

object Dependencies {

  val `executor-tools` = "com.evolutiongaming" %% "executor-tools" % "1.0.1"
  val scalatest        = "org.scalatest"       %% "scalatest"      % "3.0.8"
  val `cats-helper`    = "com.evolutiongaming" %% "cats-helper"    % "0.0.18"

  object Cats {
    private val version = "1.6.1"
    val core   = "org.typelevel" %% "cats-core"   % version
    val effect = "org.typelevel" %% "cats-effect" % "1.3.1"
  }

  object Akka {
    private val version = "2.5.23"
    val actor              = "com.typesafe.akka" %% "akka-actor"            % version
    val cluster            = "com.typesafe.akka" %% "akka-cluster"          % version
    val `distributed-data` = "com.typesafe.akka" %% "akka-distributed-data" % version
    val testkit            = "com.typesafe.akka" %% "akka-testkit"          % version
  }
}
