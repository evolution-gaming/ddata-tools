package com.evolutiongaming.cluster.ddata

import akka.actor.{Actor, ActorLogging, ActorRef, ActorRefFactory, ActorSystem, Props}
import akka.cluster.ddata.Replicator.{ReadConsistency, WriteConsistency}
import akka.cluster.ddata.{DistributedData, Key, ReplicatedData, Replicator => R}
import akka.pattern.ask
import akka.util.Timeout
import cats.effect.{Resource, Sync}
import cats.implicits._
import com.evolutiongaming.catshelper.{FromFuture, ToFuture}
import com.evolutiongaming.cluster.ddata.{ReplicatorError => E}

import scala.concurrent.{ExecutionContext, Future}
import scala.concurrent.duration.FiniteDuration

/**
  * Typesafe api for [[https://doc.akka.io/docs/akka/2.5.22/distributed-data.html]]
  *
  * Akka Distributed Data is useful when you need to share data between nodes in an Akka Cluster.
  * The data is accessed with an actor providing a key-value store like API.
  * The keys are unique identifiers with type information of the data values.
  * The values are Conflict Free Replicated Data Types (CRDTs).
  * All data entries are spread to all nodes, or nodes with a certain role, in the cluster via direct replication and gossip based dissemination.
  * You have fine grained control of the consistency level for reads and writes.
  */
trait SafeReplicator[F[_], A <: ReplicatedData] {

  /**
    * [[https://doc.akka.io/docs/akka/2.5.22/distributed-data.html#get]]
    *
    * To retrieve the current value of a data
    *
    * @param consistency [[https://doc.akka.io/docs/akka/2.5.22/distributed-data.html#consistency]]
    */
  def get(implicit consistency: ReadConsistency): F[Option[A]]

  /**
    * [[https://doc.akka.io/docs/akka/2.5.22/distributed-data.html#update]]
    *
    * To modify and replicate a data value
    * The current data value for the key of the Update is passed as parameter to the modify function of the Update.
    * The function is supposed to return the new value of the data, which will then be replicated according to the given consistency level.
    *
    * @param modify      The modify function is called by the Replicator actor and must therefore be a pure function
    *                    that only uses the data parameter and stable fields from enclosing scope.
    *                    It must for example not access the sender (sender()) reference of an enclosing actor.
    * @param consistency [[https://doc.akka.io/docs/akka/2.5.22/distributed-data.html#consistency]]
    */
  def update(modify: Option[A] => A)(implicit consistency: WriteConsistency): F[Unit]

  /**
    * [[https://doc.akka.io/docs/akka/2.5.22/distributed-data.html#delete]]
    *
    * A deleted key cannot be reused again,
    * but it is still recommended to delete unused data entries because that reduces the replication overhead when new nodes join the cluster.
    * Subsequent delete, update and get calls will fail with Deleted or AlreadyDeleted. Subscribers will stop
    *
    * @param consistency [[https://doc.akka.io/docs/akka/2.5.22/distributed-data.html#consistency]]
    */
  def delete(implicit consistency: WriteConsistency): F[Boolean]

  /**
    * [[https://doc.akka.io/docs/akka/2.5.22/distributed-data.html#subscribe]]
    *
    * Subscribers will be notified periodically with the configured notify-subscribers-interval
    *
    * @param onStop    called when the subscriber is stopped or data is deleted
    * @param onChanged called when the data is updated.
    * @return Unsubscribe function, you should call to unregister
    */
  def subscribe(
    onStop: F[Unit],
    onChanged: A => F[Unit])(implicit
    factory: ActorRefFactory,
    executor: ExecutionContext
  ): Resource[F, Unit]

  /**
    * Notifies the subscribers immediately, to not wait for periodic updates
    */
  def flushChanges: F[Unit]
}

object SafeReplicator {

  def of[F[_] : Sync : FromFuture : ToFuture, A <: ReplicatedData](
    key: Key[A],
    timeout: FiniteDuration)(implicit
    system: ActorSystem
  ): F[SafeReplicator[F, A]] = {

    for {
      replicator <- Sync[F].delay { DistributedData(system).replicator }
    } yield {
      apply[F, A](key, timeout, replicator)
    }
  }

  def apply[F[_] : Sync : FromFuture : ToFuture, A <: ReplicatedData](
    key: Key[A],
    timeout: FiniteDuration,
    replicator: ActorRef
  ): SafeReplicator[F, A] = {

    def askF(cmd: R.Command[A]): F[Any] = {
      FromFuture[F].apply { replicator.ask(cmd)(Timeout(timeout), ActorRef.noSender) }
    }

    new SafeReplicator[F, A] {

      def get(implicit consistency: ReadConsistency) = {
        val get = R.Get(key, consistency)
        askF(get).flatMap {
          case a: R.GetResponse[_] => a.asInstanceOf[R.GetResponse[A]] match {
            case a: R.GetSuccess[A] => a.dataValue.some.pure[F]
            case _: R.NotFound[A]   => none[A].pure[F]
            case _: R.GetFailure[A] => E.getFailure.raiseError[F, Option[A]]
          }
          case _: R.DataDeleted[_] => E.dataDeleted.raiseError[F, Option[A]]
          case a                   => E.unknown(s"unknown reply $a").raiseError[F, Option[A]]
        }
      }


      def update(modify: Option[A] => A)(implicit consistency: WriteConsistency) = {
        val update = R.Update(key, consistency, None)(modify)
        askF(update).flatMap {
          case a: R.UpdateResponse[_] => a.asInstanceOf[R.UpdateResponse[A]] match {
            case _: R.UpdateSuccess[A] => ().pure[F]
            case _: R.UpdateTimeout[A] => E.timeout.raiseError[F, Unit]
            case a: R.ModifyFailure[A] => E.modifyFailure(a.errorMessage, a.cause).raiseError[F, Unit]
            case _: R.StoreFailure[A]  => E.storeFailure.raiseError[F, Unit]
          }
          case _: R.DataDeleted[_]    => E.dataDeleted.raiseError[F, Unit]
          case a                      => E.unknown(s"unknown reply $a").raiseError[F, Unit]
        }
      }


      def delete(implicit consistency: WriteConsistency) = {
        val delete = R.Delete(key, consistency)
        askF(delete).flatMap {
          case a: R.DeleteResponse[_] => a.asInstanceOf[R.DeleteResponse[A]] match {
            case _: R.DeleteSuccess[A]            => true.pure[F]
            case _: R.DataDeleted[A]              => false.pure[F]
            case _: R.ReplicationDeleteFailure[A] => E.replicationFailure.raiseError[F, Boolean]
            case _: R.StoreFailure[A]             => E.storeFailure.raiseError[F, Boolean]
          }
          case a                      => E.unknown(s"unknown reply $a").raiseError[F, Boolean]
        }
      }


      def subscribe(
        onStop: F[Unit],
        onChanged: A => F[Unit])(implicit
        factory: ActorRefFactory,
        executor: ExecutionContext
      ) = {

        def actor() = new Actor with ActorLogging {

          var future = Future.unit

          val handleError = (error: Throwable) => {
            Sync[F].delay { log.error(error, s"$key onChanged failed $error") }
          }

          private def rcvChanged(a: A): Unit = {
            val effect = onChanged(a).handleErrorWith(handleError)
            future = future.flatMap { _ => ToFuture[F].apply(effect) }
          }

          override def preStart(): Unit = {
            replicator.tell(R.Subscribe(key, self), self)
            super.preStart()
          }

          def receive = {
            case a @ R.Changed(`key`)    => rcvChanged(a.get(key))
            case R.DataDeleted(`key`, _) => context.stop(self)
            case a                       => log.warning(s"$key unexpected $a")
          }

          override def postStop(): Unit = {
            future.flatMap { _ => ToFuture[F].apply { onStop } }
            replicator.tell(R.Unsubscribe(key, self), self)
            super.postStop()
          }
        }

        val props = Props(actor())

        val result = for {
          ref <- Sync[F].delay { factory.actorOf(props) }
        } yield {
          val release = Sync[F].delay { factory.stop(ref) }
          ((), release)
        }
        Resource(result)
      }

      def flushChanges = {
        Sync[F].delay { replicator ! R.FlushChanges }
      }
    }
  }
}
