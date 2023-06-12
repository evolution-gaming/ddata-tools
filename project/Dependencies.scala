import sbt._

object Dependencies {

  val `executor-tools` = "com.evolutiongaming" %% "executor-tools" % "1.0.2"
  val scalatest        = "org.scalatest"       %% "scalatest"      % "3.0.8"
  val `cats-helper`    = "com.evolutiongaming" %% "cats-helper"    % "3.5.0"
  val smetrics         = "com.evolutiongaming" %% "smetrics"       % "1.3.0"

  object Cats {
    val core   = "org.typelevel" %% "cats-core"   % "2.9.0"
    val effect = "org.typelevel" %% "cats-effect" % "3.4.8"
  }

  object Akka {
    private val version = "2.6.20"
    val actor              = "com.typesafe.akka" %% "akka-actor"            % version
    val cluster            = "com.typesafe.akka" %% "akka-cluster"          % version
    val `distributed-data` = "com.typesafe.akka" %% "akka-distributed-data" % version
    val testkit            = "com.typesafe.akka" %% "akka-testkit"          % version
  }
}
