package com.evolutiongaming.cluster.ddata

import scala.util.control.NoStackTrace

sealed abstract class ReplicatorError extends RuntimeException with NoStackTrace

object ReplicatorError {

  def dataDeleted: ReplicatorError = DataDeleted

  def getFailure: ReplicatorError = GetFailure

  def replicationFailure: ReplicatorError = ReplicationFailure

  def storeFailure: ReplicatorError = StoreFailure

  def timeout: ReplicatorError = Timeout

  def modifyFailure(msg: String, cause: Throwable): ReplicatorError = ModifyFailure(msg, cause)

  def unknown(msg: String): ReplicatorError = Unknown(msg)


  case object DataDeleted extends ReplicatorError

  case object GetFailure extends ReplicatorError

  case object StoreFailure extends ReplicatorError

  case object ReplicationFailure extends ReplicatorError

  case object Timeout extends ReplicatorError

  final case class Unknown(msg: String) extends ReplicatorError

  final case class ModifyFailure(msg: String, cause: Throwable) extends ReplicatorError
}
