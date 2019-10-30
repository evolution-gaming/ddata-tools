import sbt._

object Dependencies {

  val `executor-tools` = "com.evolutiongaming" %% "executor-tools" % "1.0.2"
  val scalatest        = "org.scalatest"       %% "scalatest"      % "3.0.8"
  val `cats-helper`    = "com.evolutiongaming" %% "cats-helper"    % "1.0.4"
  val smetrics         = "com.evolutiongaming" %% "smetrics"       % "0.0.7"

  object Cats {
    private val version = "2.0.0"
    val core   = "org.typelevel" %% "cats-core"   % version
    val effect = "org.typelevel" %% "cats-effect" % "2.0.0"
  }

  object Akka {
    private val version = "2.5.26"
    val actor              = "com.typesafe.akka" %% "akka-actor"            % version
    val cluster            = "com.typesafe.akka" %% "akka-cluster"          % version
    val `distributed-data` = "com.typesafe.akka" %% "akka-distributed-data" % version
    val testkit            = "com.typesafe.akka" %% "akka-testkit"          % version
  }
}
