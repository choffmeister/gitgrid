package com.gitgrid.workers

import java.util.concurrent.TimeUnit

import akka.actor._
import akka.testkit._

import org.specs2.mutable._

import scala.concurrent._
import scala.concurrent.duration.FiniteDuration

class TestActorSystem extends TestKit(ActorSystem()) with ImplicitSender with After {
  def after = system.shutdown()
}

class WorkerSpec extends Specification {
  import WorkerProtocol._

  def square(item: Any) = Future.successful(item.asInstanceOf[Int] * item.asInstanceOf[Int])
  def double(item: Any) = Future.successful(item.asInstanceOf[Int] * 2)
  def fail(item: Any) = Future.failed[Any](new Exception())

  "Worker" should {
    "consume work items" in new TestActorSystem {
      val master = system.actorOf(Props(new WorkerMaster), "worker-master")
      val slave1 = system.actorOf(Props(new WorkerSlave(system.actorSelection(master.path), square)), "worker-slave-1")
      val slave2 = system.actorOf(Props(new WorkerSlave(system.actorSelection(master.path), square)), "worker-slave-2")

      master ! Work(10)
      expectMsg(WorkResult(10, 100))
      master ! Work(20)
      expectMsg(WorkResult(20, 400))
      master ! Work(30)
      expectMsg(WorkResult(30, 900))

      system.stop(slave1)
      system.stop(slave2)
      Thread.sleep(100L)
      master ! Work(40)
      expectNoMsg(FiniteDuration(100, TimeUnit.MILLISECONDS))

      val slave3 = system.actorOf(Props(new WorkerSlave(system.actorSelection(master.path), double)), "worker-slave-3")

      expectMsg(WorkResult(40, 80))
    }

    "recover from single failure" in new TestActorSystem {
      val master = system.actorOf(Props(new WorkerMaster), "worker-master")
      val slave1 = system.actorOf(Props(new WorkerSlave(system.actorSelection(master.path), fail)), "worker-slave-1")

      master ! Work(10)
      expectNoMsg(FiniteDuration(100, TimeUnit.MILLISECONDS))
      system.stop(slave1)
      Thread.sleep(100L)

      val slave2 = system.actorOf(Props(new WorkerSlave(system.actorSelection(master.path), square)), "worker-slave-2")
      expectMsg(WorkResult(10, 100))
    }
  }
}
