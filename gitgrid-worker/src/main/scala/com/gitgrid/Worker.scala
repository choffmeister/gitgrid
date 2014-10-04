package com.gitgrid

import java.util.concurrent.TimeUnit

import akka.actor.SupervisorStrategy.Restart
import akka.actor._
import akka.pattern.ask
import akka.routing.{RoundRobinPool, RoundRobinRouter}
import akka.util.Timeout
import com.gitgrid.Tasks._

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent._
import scala.concurrent.duration._

class Worker extends Bootable {
  val system = ActorSystem("gitgrid-worker", CoreConfig.raw)
  val coreConf = CoreConfig.load()

  def startup() = {
    val workerMaster = system.actorSelection("akka.tcp://gitgrid-server@localhost:7915/user/worker-master")
    val workerSlave = system.actorOf(RoundRobinPool(10, supervisorStrategy = OneForOneStrategy() {
      case _ => Restart
    }).props(Props(new WorkerSlave(workerMaster, work))), "worker-slaves")
  }

  def shutdown() = {
    system.shutdown()
    system.awaitTermination(1.seconds)
  }

  implicit val timeout = Timeout(120, TimeUnit.SECONDS)
  val oneMinute = system.actorOf(Props[OneMinuteActor])

  def work(item: Any): Future[Any] = Future.successful[Any](item).flatMap {
    case BuildFromCommit(projectId, commit, ownerName, projectName) =>
      println(projectId)
      println(commit)
      oneMinute ? OneMinuteActor.Tick(0)

    case x =>
      println(x)
      throw new Exception(s"Unknown task type $x")
  }
}

object Worker {
  def main(args: Array[String]) {
    val app = new Worker()
    app.startup()
  }
}

trait Bootable {
  def startup(): Unit
  def shutdown(): Unit

  sys.ShutdownHookThread(shutdown())
}

class OneMinuteActor extends Actor {
  import OneMinuteActor._

  def receive = {
    case Tick(i, None) =>
      self ! Tick(i, Some(sender()))

    case Tick(i, Some(s)) if i < 60 =>
      println(">>> " + Tick(i, Some(s)))
      context.system.scheduler.scheduleOnce(FiniteDuration(1, TimeUnit.SECONDS), self, Tick(i + 1, Some(s)))
    case Tick(i, Some(s)) =>
      s ! Tick(i, Some(s))
  }

}

object OneMinuteActor {
  case class Tick(n: Int, sender: Option[ActorRef] = None)
}
