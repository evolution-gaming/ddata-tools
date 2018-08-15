package com.evolutiongaming.cluster.ddata

import akka.actor.{Actor, ActorLogging, ActorRef, ActorRefFactory, ActorSystem, Props}
import akka.cluster.ddata.Replicator.{ReadConsistency, WriteConsistency}
import akka.cluster.ddata.{DistributedData, Key, ReplicatedData, Replicator => R}
import akka.pattern.ask
import akka.util.Timeout
import com.evolutiongaming.concurrent.CurrentThreadExecutionContext

import scala.concurrent.Future
import scala.concurrent.duration.FiniteDuration

/**
  * Typesafe api for [[https://doc.akka.io/docs/akka/2.5.9/distributed-data.html]]
  *
  * Akka Distributed Data is useful when you need to share data between nodes in an Akka Cluster.
  * The data is accessed with an actor providing a key-value store like API.
  * The keys are unique identifiers with type information of the data values.
  * The values are Conflict Free Replicated Data Types (CRDTs).
  * All data entries are spread to all nodes, or nodes with a certain role, in the cluster via direct replication and gossip based dissemination.
  * You have fine grained control of the consistency level for reads and writes.
  */
trait SafeReplicator[T <: ReplicatedData] {
  import SafeReplicator._

  /**
    * [[https://doc.akka.io/docs/akka/2.5.9/distributed-data.html#get]]
    *
    * To retrieve the current value of a data
    *
    * @param consistency [[https://doc.akka.io/docs/akka/2.5.9/distributed-data.html#consistency]]
    */
  def get()(implicit consistency: ReadConsistency): Future[Either[GetFailure, T]]

  /**
    * [[https://doc.akka.io/docs/akka/2.5.9/distributed-data.html#update]]
    *
    * To modify and replicate a data value
    * The current data value for the key of the Update is passed as parameter to the modify function of the Update.
    * The function is supposed to return the new value of the data, which will then be replicated according to the given consistency level.
    *
    * @param modify      The modify function is called by the Replicator actor and must therefore be a pure function
    *                    that only uses the data parameter and stable fields from enclosing scope.
    *                    It must for example not access the sender (sender()) reference of an enclosing actor.
    * @param consistency [[https://doc.akka.io/docs/akka/2.5.9/distributed-data.html#consistency]]
    */
  def update(modify: Option[T] => T)(implicit consistency: WriteConsistency): Future[Either[UpdateFailure, Unit]]

  /**
    * [[https://doc.akka.io/docs/akka/2.5.9/distributed-data.html#delete]]
    *
    * A deleted key cannot be reused again,
    * but it is still recommended to delete unused data entries because that reduces the replication overhead when new nodes join the cluster.
    * Subsequent delete, update and get calls will fail with Deleted or AlreadyDeleted. Subscribers will stop
    *
    * @param consistency [[https://doc.akka.io/docs/akka/2.5.9/distributed-data.html#consistency]]
    */
  def delete()(implicit consistency: WriteConsistency): Future[Either[DeleteFailure, Unit]]

  /**
    * [[https://doc.akka.io/docs/akka/2.5.9/distributed-data.html#subscribe]]
    *
    * Subscribers will be notified periodically with the configured notify-subscribers-interval
    *
    * @param onStop    called when the subscriber is stopped or data is deleted
    * @param onChanged called when the data is updated.
    * @return Unsubscribe function, you should call to unregister
    */
  def subscribe(onStop: () => Unit = () => ())(onChanged: T => Unit)(implicit factory: ActorRefFactory): Unsubscribe

  /**
    * Notifies the subscribers immediately, to not wait for periodic updates
    */
  def flushChanges(): Unit
}

object SafeReplicator {

  private lazy val unit = ().asRight
  
  type Unsubscribe = () => Unit


  def apply[T <: ReplicatedData](key: Key[T], timeout: FiniteDuration)
    (implicit system: ActorSystem): SafeReplicator[T] = {

    val replicator = DistributedData(system).replicator
    apply(key, timeout, replicator)
  }

  def apply[T <: ReplicatedData](key: Key[T], timeout: FiniteDuration, replicator: ActorRef): SafeReplicator[T] = {

    implicit val ec = CurrentThreadExecutionContext
    implicit val akkaTimeout = Timeout(timeout)

    new SafeReplicator[T] {

      def get()(implicit consistency: ReadConsistency) = {
        val get = R.Get(key, consistency)
        replicator.ask(get) map {
          case R.DataDeleted(`key`, _) => GetFailure.Deleted.asLeft
          case x                       => x.asInstanceOf[R.GetResponse[T]] match {
            case x: R.GetSuccess[T] => x.dataValue.asRight
            case _: R.NotFound[T]   => GetFailure.NotFound.asLeft
            case _: R.GetFailure[T] => GetFailure.Failure.asLeft
          }
        }
      }

      def update(modify: Option[T] => T)(implicit consistency: WriteConsistency) = {
        val update = R.Update(key, consistency, None)(modify)
        replicator.ask(update) map {
          case R.DataDeleted(`key`, _) => UpdateFailure.Deleted.asLeft
          case x                       => x.asInstanceOf[R.UpdateResponse[T]] match {
            case _: R.UpdateSuccess[T] => unit
            case _: R.UpdateTimeout[T] => UpdateFailure.Timeout.asLeft
            case x: R.ModifyFailure[T] => UpdateFailure.Failure(x.errorMessage, x.cause).asLeft
            case _: R.StoreFailure[T]  => UpdateFailure.StoreFailure.asLeft
          }
        }
      }

      def delete()(implicit consistency: WriteConsistency) = {
        val delete = R.Delete(key, consistency)
        replicator.ask(delete).mapTo[R.DeleteResponse[T]] map {
          case _: R.DeleteSuccess[T]            => unit
          case _: R.DataDeleted[T]              => DeleteFailure.AlreadyDeleted.asLeft
          case _: R.ReplicationDeleteFailure[T] => DeleteFailure.ReplicationFailure.asLeft
          case _: R.StoreFailure[T]             => DeleteFailure.StoreFailure.asLeft
        }
      }

      def subscribe(onStop: () => Unit = () => ())(onChanged: T => Unit)(implicit factory: ActorRefFactory): Unsubscribe = {
        def actor = new Actor with ActorLogging {
          replicator ! R.Subscribe(key, self)
          def receive = {
            case x @ R.Changed(`key`)    => onChanged(x get key)
            case R.DataDeleted(`key`, _) => context stop self
            case x                       => log warning s"$key unexpected $x"
          }
          override def postStop(): Unit = {
            onStop()
            replicator ! R.Unsubscribe(key, self)
            super.postStop()
          }
        }

        val props = Props(actor)
        val ref = factory.actorOf(props)
        () => factory stop ref
      }

      def flushChanges(): Unit = {
        replicator ! R.FlushChanges
      }
    }
  }


  sealed trait GetFailure

  object GetFailure {
    case object NotFound extends GetFailure
    case object Failure extends GetFailure
    case object Deleted extends GetFailure
  }


  sealed trait UpdateFailure

  object UpdateFailure {
    final case class Failure(message: String, cause: Throwable) extends UpdateFailure
    case object Timeout extends UpdateFailure
    case object StoreFailure extends UpdateFailure
    case object Deleted extends UpdateFailure
  }


  sealed trait DeleteFailure

  object DeleteFailure {
    case object AlreadyDeleted extends DeleteFailure
    case object ReplicationFailure extends DeleteFailure
    case object StoreFailure extends DeleteFailure
  }

  private implicit class EitherOps[A](val self: A) extends AnyVal {
    def asLeft[B]: Either[A, B] = Left(self)
    def asRight[B]: Either[B, A] = Right(self)
  }
}
