package com.evolutiongaming.cluster.ddata

import akka.cluster.ddata.Replicator.{ReadLocal, WriteLocal}
import akka.cluster.ddata.{GCounter, GCounterKey, Replicator => R}
import com.evolutiongaming.cluster.ddata.SafeReplicator.{DeleteFailure, GetFailure, UpdateFailure}
import org.scalatest.{Matchers, WordSpec}

import scala.concurrent.duration._
import scala.util.Success
import scala.util.control.NoStackTrace

class SafeReplicatorSpec extends WordSpec with ActorSpec with Matchers {

  "proxy get" in new Scope {
    val result = replicator.get()
    expectMsg(R.Get(key, readConsistency))
    lastSender ! R.GetSuccess(key, None)(counter)
    result.value shouldEqual Some(Success(Right(counter)))
  }

  "proxy get and reply with DataDeleted" in new Scope {
    val result = replicator.get()
    expectMsg(R.Get(key, readConsistency))
    lastSender ! R.DataDeleted(key, None)
    result.value shouldEqual Some(Success(Left(GetFailure.Deleted)))
  }

  "proxy get and reply with NotFound" in new Scope {
    val result = replicator.get()
    expectMsg(R.Get(key, readConsistency))
    lastSender ! R.NotFound(key, None)
    result.value shouldEqual Some(Success(Left(GetFailure.NotFound)))
  }

  "proxy get and reply with GetFailure" in new Scope {
    val result = replicator.get()
    expectMsg(R.Get(key, readConsistency))
    lastSender ! R.GetFailure(key, None)
    result.value shouldEqual Some(Success(Left(GetFailure.Failure)))
  }

  "proxy update" in new Scope {
    val modify: Option[GCounter] => GCounter = _ => counter
    val result = replicator.update(modify)
    expectMsg(R.Update(key, writeConsistency, None)(modify))
    lastSender ! R.UpdateSuccess(key, None)
    result.value shouldEqual Some(Success(Right(())))
  }

  "proxy update and reply with Failure" in new Scope {
    val modify: Option[GCounter] => GCounter = _ => counter
    val result = replicator.update(modify)
    expectMsg(R.Update(key, writeConsistency, None)(modify))
    lastSender ! R.ModifyFailure(key, "errorMsg", failure, None)
    result.value shouldEqual Some(Success(Left(UpdateFailure.Failure("errorMsg", failure))))
  }

  "proxy update and reply with Timeout" in new Scope {
    val modify: Option[GCounter] => GCounter = _ => counter
    val result = replicator.update(modify)
    expectMsg(R.Update(key, writeConsistency, None)(modify))
    lastSender ! R.UpdateTimeout(key, None)
    result.value shouldEqual Some(Success(Left(UpdateFailure.Timeout)))
  }

  "proxy update and reply with StoreFailure" in new Scope {
    val modify: Option[GCounter] => GCounter = _ => counter
    val result = replicator.update(modify)
    expectMsg(R.Update(key, writeConsistency, None)(modify))
    lastSender ! R.StoreFailure(key, None)
    result.value shouldEqual Some(Success(Left(UpdateFailure.StoreFailure)))
  }

  "proxy update and reply with Deleted" in new Scope {
    val modify: Option[GCounter] => GCounter = _ => counter
    val result = replicator.update(modify)
    expectMsg(R.Update(key, writeConsistency, None)(modify))
    lastSender ! R.DataDeleted(key, None)
    result.value shouldEqual Some(Success(Left(UpdateFailure.Deleted)))
  }

  "proxy delete" in new Scope {
    val result = replicator.delete()
    expectMsg(R.Delete(key, writeConsistency, None))
    lastSender ! R.DeleteSuccess(key, None)
    result.value shouldEqual Some(Success(Right(())))
  }

  "proxy delete and reply with AlreadyDeleted" in new Scope {
    val result = replicator.delete()
    expectMsg(R.Delete(key, writeConsistency, None))
    lastSender ! R.DataDeleted(key, None)
    result.value shouldEqual Some(Success(Left(DeleteFailure.AlreadyDeleted)))
  }

  "proxy delete and reply with ReplicationFailure" in new Scope {
    val result = replicator.delete()
    expectMsg(R.Delete(key, writeConsistency, None))
    lastSender ! R.ReplicationDeleteFailure(key, None)
    result.value shouldEqual Some(Success(Left(DeleteFailure.ReplicationFailure)))
  }

  "proxy delete and reply with StoreFailure" in new Scope {
    val result = replicator.delete()
    expectMsg(R.Delete(key, writeConsistency, None))
    lastSender ! R.StoreFailure(key, None)
    result.value shouldEqual Some(Success(Left(DeleteFailure.StoreFailure)))
  }

  "proxy subscribe" in new Scope {
    val unsubscribe = replicator.subscribe() { testActor ! _ }
    val ref = expectMsgPF() { case R.Subscribe(`key`, ref) => ref }
    ref ! R.Changed(key)(counter)
    expectMsg(counter)
    unsubscribe()
    expectMsg(R.Unsubscribe(key, ref))
  }

  "proxy subscribe and stop when data deleted" in new Scope {
    replicator.subscribe() { testActor ! _ }
    val ref = expectMsgPF() { case R.Subscribe(`key`, ref) => ref }
    ref ! R.DataDeleted(key, None)
    expectMsg(R.Unsubscribe(key, ref))
  }

  "proxy flushChanges" in new Scope {
    replicator.flushChanges()
    expectMsg(R.FlushChanges)
  }

  private trait Scope extends ActorScope {
    implicit val readConsistency = ReadLocal
    implicit val writeConsistency = WriteLocal
    val key = GCounterKey("key")
    val counter = GCounter.empty

    val replicator = SafeReplicator(key, 5.seconds, testActor)
  }

  private val failure = new RuntimeException with NoStackTrace
}
