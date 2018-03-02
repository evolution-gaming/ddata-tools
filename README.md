# DData Tools [![Build Status](https://travis-ci.org/evolution-gaming/ddata-tools.svg)](https://travis-ci.org/evolution-gaming/ddata-tools) [![Coverage Status](https://coveralls.io/repos/evolution-gaming/ddata-tools/badge.svg)](https://coveralls.io/r/evolution-gaming/ddata-tools) [![Codacy Badge](https://api.codacy.com/project/badge/Grade/91f43a46edcf44e7829e4ef10aae3ba1)](https://www.codacy.com/app/evolution-gaming/ddata-tools?utm_source=github.com&amp;utm_medium=referral&amp;utm_content=evolution-gaming/ddata-tools&amp;utm_campaign=Badge_Grade) [ ![version](https://api.bintray.com/packages/evolutiongaming/maven/ddata-tools/images/download.svg) ](https://bintray.com/evolutiongaming/maven/ddata-tools/_latestVersion) [![License: MIT](https://img.shields.io/badge/License-MIT-yellowgreen.svg)](https://opensource.org/licenses/MIT)

### SafeReplicator - is a typesafe api for [DData replicator](https://doc.akka.io/docs/akka/2.5.9/distributed-data.html)

```scala
trait SafeReplicator[T <: ReplicatedData] {
  def get()(implicit consistency: ReadConsistency): Future[Either[GetFailure, T]]
  def update(modify: Option[T] => T)(implicit consistency: WriteConsistency): Future[Either[UpdateFailure, Unit]]
  def delete()(implicit consistency: WriteConsistency): Future[Either[DeleteFailure, Unit]]
  def subscribe(onStop: () => Unit = () => ())(onChanged: T => Unit)(implicit factory: ActorRefFactory): Unsubscribe
  def flushChanges(): Unit
}
```

## Setup

```scala
resolvers += Resolver.bintrayRepo("evolutiongaming", "maven")

libraryDependencies += "com.evolutiongaming" %% "ddata-tools" % "0.0.1"
```
