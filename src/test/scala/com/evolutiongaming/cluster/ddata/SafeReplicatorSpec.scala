package com.evolutiongaming.cluster.ddata

import akka.cluster.ddata.Replicator.{ReadLocal, WriteLocal}
import akka.cluster.ddata.{GCounter, GCounterKey, Replicator => R}
import cats.effect.IO
import cats.implicits._
import com.evolutiongaming.cluster.ddata.IOSuite._
import org.scalatest.{Matchers, WordSpec}

import scala.concurrent.duration._
import scala.concurrent.{Await, Future}
import scala.util.control.NoStackTrace
import scala.util.{Failure, Success, Try}

class SafeReplicatorSpec extends WordSpec with ActorSpec with Matchers {
  import SafeReplicatorSpec._

  private implicit val readConsistency = ReadLocal
  private implicit val writeConsistency = WriteLocal

  "proxy get" in new Scope {
    val result = replicator.get.unsafeToFuture()
    expectMsg(R.Get(key, readConsistency))
    lastSender ! R.GetSuccess(key, None)(counter)
    result.await shouldEqual Success(Some(counter))
  }

  "proxy get and reply with DataDeleted" in new Scope {
    val result = replicator.get.unsafeToFuture()
    expectMsg(R.Get(key, readConsistency))
    lastSender ! R.DataDeleted(key, None)
    result.await shouldEqual Failure(ReplicatorError.dataDeleted)
  }

  "proxy get and reply with NotFound" in new Scope {
    val result = replicator.get.unsafeToFuture()
    expectMsg(R.Get(key, readConsistency))
    lastSender ! R.NotFound(key, None)
    result.await shouldEqual Success(None)
  }

  "proxy get and reply with GetFailure" in new Scope {
    val result = replicator.get.unsafeToFuture()
    expectMsg(R.Get(key, readConsistency))
    lastSender ! R.GetFailure(key, None)
    result.await shouldEqual Failure(ReplicatorError.getFailure)
  }

  "proxy update" in new Scope {
    val modify: Option[GCounter] => GCounter = _ => counter
    val result = replicator.update(modify).unsafeToFuture()
    expectMsg(R.Update(key, writeConsistency, None)(modify))
    lastSender ! R.UpdateSuccess(key, None)
    result.await shouldEqual Success(())
  }

  "proxy update and reply with Failure" in new Scope {
    val modify: Option[GCounter] => GCounter = _ => counter
    val result = replicator.update(modify).unsafeToFuture()
    expectMsg(R.Update(key, writeConsistency, None)(modify))
    lastSender ! R.ModifyFailure(key, "errorMsg", failure, None)
    result.await shouldEqual Failure(ReplicatorError.modifyFailure("errorMsg", failure))
  }

  "proxy update and reply with Timeout" in new Scope {
    val modify: Option[GCounter] => GCounter = _ => counter
    val result = replicator.update(modify).unsafeToFuture()
    expectMsg(R.Update(key, writeConsistency, None)(modify))
    lastSender ! R.UpdateTimeout(key, None)
    result.await shouldEqual Failure(ReplicatorError.timeout)
  }

  "proxy update and reply with StoreFailure" in new Scope {
    val modify: Option[GCounter] => GCounter = _ => counter
    val result = replicator.update(modify).unsafeToFuture()
    expectMsg(R.Update(key, writeConsistency, None)(modify))
    lastSender ! R.StoreFailure(key, None)
    result.await shouldEqual Failure(ReplicatorError.storeFailure)
  }

  "proxy update and reply with Deleted" in new Scope {
    val modify: Option[GCounter] => GCounter = _ => counter
    val result = replicator.update(modify).unsafeToFuture()
    expectMsg(R.Update(key, writeConsistency, None)(modify))
    lastSender ! R.DataDeleted(key, None)
    result.await shouldEqual Failure(ReplicatorError.dataDeleted)
  }

  "proxy delete" in new Scope {
    val result = replicator.delete.unsafeToFuture()
    expectMsg(R.Delete(key, writeConsistency, None))
    lastSender ! R.DeleteSuccess(key, None)
    result.await shouldEqual Success(true)
  }

  "proxy delete and reply with AlreadyDeleted" in new Scope {
    val result = replicator.delete.unsafeToFuture()
    expectMsg(R.Delete(key, writeConsistency, None))
    lastSender ! R.DataDeleted(key, None)
    result.await shouldEqual Success(false)
  }

  "proxy delete and reply with ReplicationFailure" in new Scope {
    val result = replicator.delete.unsafeToFuture()
    expectMsg(R.Delete(key, writeConsistency, None))
    lastSender ! R.ReplicationDeleteFailure(key, None)
    result.await shouldEqual Failure(ReplicatorError.replicationFailure)
  }

  "proxy delete and reply with StoreFailure" in new Scope {
    val result = replicator.delete.unsafeToFuture()
    expectMsg(R.Delete(key, writeConsistency, None))
    lastSender ! R.StoreFailure(key, None)
    result.await shouldEqual Failure(ReplicatorError.storeFailure)
  }

  "proxy subscribe" in new Scope {
    val (_, unsubscribe) = replicator.subscribe(().pure[IO], onChanged).allocated.unsafeRunSync()
    val ref = expectMsgPF() { case R.Subscribe(`key`, ref) => ref }
    ref ! R.Changed(key)(counter)
    expectMsg(counter)
    unsubscribe.unsafeRunSync()
    expectMsg(R.Unsubscribe(key, ref))
  }

  "proxy subscribe and stop when data deleted" in new Scope {
    replicator.subscribe(().pure[IO], onChanged).allocated.unsafeRunSync()
    val ref = expectMsgPF() { case R.Subscribe(`key`, ref) => ref }
    ref ! R.DataDeleted(key, None)
    expectMsg(R.Unsubscribe(key, ref))
  }

  "proxy flushChanges" in new Scope {
    replicator.flushChanges.unsafeRunSync()
    expectMsg(R.FlushChanges)
  }

  private trait Scope extends ActorScope {
    val key = GCounterKey("key")
    val counter = GCounter.empty
    val onChanged = (data: GCounter) => IO { testActor ! data }
    val replicator = SafeReplicator[IO, GCounter](key, 5.seconds, testActor)
  }

  private val failure = new RuntimeException with NoStackTrace
}

object SafeReplicatorSpec {

  implicit class FutureOps[A](val self: Future[A]) extends AnyVal {

    def await: Try[A] = Try { Await.result(self, 3.seconds) }
  }
}