package com.gitgrid

import akka.actor._
import com.gitgrid.Tasks._

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent._
import scala.concurrent.duration._

class Worker extends Bootable {
  val system = ActorSystem("gitgrid-worker", CoreConfig.raw)
  val coreConf = CoreConfig.load()

  def startup() = {
    val workerMaster = system.actorSelection("akka.tcp://gitgrid-server@localhost:7915/user/worker-master")
    val workerSlave = system.actorOf(Props(new WorkerSlave(workerMaster, work)), "worker-slave")
  }

  def shutdown() = {
    system.shutdown()
    system.awaitTermination(1.seconds)
  }

  def work(item: Any): Future[Any] = Future.successful[Any](item).map {
    case BuildFromCommit(commit) =>
      println(commit)
      for (i <- 1 to 100) {
        println(">>> step " + i)
        Thread.sleep(1000L)
      }
      true

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
