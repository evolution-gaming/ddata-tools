# DData Tools
[![Build Status](https://github.com/evolution-gaming/ddata-tools/workflows/CI/badge.svg)](https://github.com/evolution-gaming/ddata-tools/actions?query=workflow%3ACI)
[![Coverage Status](https://coveralls.io/repos/github/evolution-gaming/ddata-tools/badge.svg?branch=master)](https://coveralls.io/github/evolution-gaming/ddata-tools?branch=master)
[![Codacy Badge](https://api.codacy.com/project/badge/Grade/91f43a46edcf44e7829e4ef10aae3ba1)](https://www.codacy.com/app/evolution-gaming/ddata-tools?utm_source=github.com&amp;utm_medium=referral&amp;utm_content=evolution-gaming/ddata-tools&amp;utm_campaign=Badge_Grade)
[![Version](https://img.shields.io/badge/version-click-blue)](https://evolution.jfrog.io/artifactory/api/search/latestVersion?g=com.evolutiongaming&a=ddata-tools_2.13&repos=public)
[![License: MIT](https://img.shields.io/badge/License-MIT-yellowgreen.svg)](https://opensource.org/licenses/MIT)

### SafeReplicator - is a typesafe api for [DData replicator](https://doc.akka.io/docs/akka/2.5.9/distributed-data.html)

```scala
trait SafeReplicator[F[_], A <: ReplicatedData] {

  def get(implicit consistency: ReadConsistency): F[Option[A]]

  def update(modify: Option[A] => A)(implicit consistency: WriteConsistency): F[Unit]

  def delete(implicit consistency: WriteConsistency): F[Boolean]

  def subscribe(
    onStop: F[Unit],
    onChanged: A => F[Unit])(implicit
    factory: ActorRefFactory,
    executor: ExecutionContext
  ): Resource[F, Unit]

  def flushChanges: F[Unit]
}
```

## Setup

```scala
addSbtPlugin("com.evolution" % "sbt-artifactory-plugin" % "0.0.2")

libraryDependencies += "com.evolutiongaming" %% "ddata-tools" % "2.0.8"
```
