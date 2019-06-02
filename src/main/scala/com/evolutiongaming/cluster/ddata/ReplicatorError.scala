package com.evolutiongaming.cluster.ddata

import cats.implicits._

import scala.util.control.NoStackTrace

sealed abstract class ReplicatorError(msg: Option[String], cause: Option[Throwable])
  extends RuntimeException(msg.orNull, cause.orNull) with NoStackTrace

object ReplicatorError {

  def dataDeleted: ReplicatorError = DataDeleted

  def getFailure: ReplicatorError = GetFailure

  def replicationFailure: ReplicatorError = ReplicationFailure

  def storeFailure: ReplicatorError = StoreFailure

  def timeout: ReplicatorError = Timeout

  def modifyFailure(msg: String, cause: Throwable): ReplicatorError = ModifyFailure(msg, cause)

  def unknown(msg: String): ReplicatorError = Unknown(msg)


  case object DataDeleted extends ReplicatorError(none, none)

  case object GetFailure extends ReplicatorError(none, none)

  case object StoreFailure extends ReplicatorError(none, none)

  case object ReplicationFailure extends ReplicatorError(none, none)

  case object Timeout extends ReplicatorError(none, none)

  final case class Unknown(msg: String) extends ReplicatorError(msg.some, none)

  final case class ModifyFailure(msg: String, cause: Throwable) extends ReplicatorError(msg.some, cause.some)
}
