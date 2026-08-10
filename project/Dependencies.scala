import sbt._

object Dependencies {

  val `executor-tools` = "com.evolutiongaming" %% "executor-tools" % "1.0.5"
  val scalatest = "org.scalatest" %% "scalatest" % "3.2.19"
  val `cats-helper` = "com.evolutiongaming" %% "cats-helper" % "3.12.0"
  val smetrics = "com.evolutiongaming" %% "smetrics" % "2.3.2"

  object Cats {
    val core = "org.typelevel" %% "cats-core" % "2.13.0"
    val effect = "org.typelevel" %% "cats-effect" % "3.5.7"
  }

  object Akka {
    private val version = "2.6.21"
    val actor = "com.typesafe.akka" %% "akka-actor" % version
    val cluster = "com.typesafe.akka" %% "akka-cluster" % version
    val `distributed-data` = "com.typesafe.akka" %% "akka-distributed-data" % version
    val testkit = "com.typesafe.akka" %% "akka-testkit" % version
  }
}
