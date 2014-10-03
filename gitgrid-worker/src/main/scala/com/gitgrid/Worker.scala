package com.gitgrid

import akka.actor._
import com.gitgrid.WorkerProtocol._

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent._
import scala.concurrent.duration._

class Worker extends Bootable {
  val system = ActorSystem("gitgrid-worker", CoreConfig.raw)
  val coreConf = CoreConfig.load()

  def startup() = {
    val workerMaster = system.actorSelection("akka.tcp://gitgrid-server@localhost:7915/user/worker-master")
    val workerSlave = system.actorOf(Props(new WorkerSlave(workerMaster, work)), "worker-slave")

    workerMaster ! Work("Hello World")
  }

  def shutdown() = {
    system.shutdown()
    system.awaitTermination(1.seconds)
  }

  def work(item: Any): Future[Any] = Future {
    println("STARTING WORK " + item)
    Thread.sleep(2500L)
    println("FINISHED WORK " + item)
    "Work is done"
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
