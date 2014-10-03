package com.gitgrid

import java.util.concurrent.TimeUnit

import akka.actor._

import scala.concurrent.duration._

class Worker extends Bootable {
  val system = ActorSystem("gitgrid-worker", CoreConfig.raw)
  val coreConf = CoreConfig.load()

  def startup() = {
    val server = system.actorSelection("akka.tcp://gitgrid-server@localhost:7915/user/worker-master")
    val client = system.actorOf(Props(new RemoteClientActor(server)), "worker-slave")
  }

  def shutdown() = {
    system.shutdown()
    system.awaitTermination(1.seconds)
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

class RemoteClientActor(server: ActorSelection) extends Actor with ActorLogging {
  server ! Identify
  self ! Tick(0)

  def receive = {
    case Tick(n) =>
      server ! "This is message " + n
      context.system.scheduler.scheduleOnce(FiniteDuration(1, TimeUnit.SECONDS), self, Tick(n + 1))(context.dispatcher)

    case x => log.info("{}", x)
  }

  case class Tick(n: Int)
}
