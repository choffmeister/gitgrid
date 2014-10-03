package com.gitgrid

import akka.actor._
import akka.io.IO
import com.gitgrid.http.HttpServiceActor
import com.gitgrid.models.Database
import spray.can.Http

import scala.concurrent.duration._

class Server extends Bootable {
  val system = ActorSystem("gitgrid-server", CoreConfig.raw)
  val coreConf = CoreConfig.load()
  val httpConf = HttpConfig.load()

  def startup() = {
    val db = Database.open(coreConf.mongoDbServers, coreConf.mongoDbDatabaseName)(system.dispatcher)

    val workerMaster = system.actorOf(Props[RemoteServerActor], "worker-master")

    val httpServer = system.actorOf(Props(new HttpServiceActor(coreConf, httpConf, db)), "http-server")
    IO(Http)(system) ! Http.Bind(httpServer, interface = httpConf.interface, port = httpConf.port)
  }

  def shutdown() = {
    system.shutdown()
    system.awaitTermination(1.seconds)
  }
}

object Server {
  def main(args: Array[String]) {
    val app = new Server()
    app.startup()
  }
}

trait Bootable {
  def startup(): Unit
  def shutdown(): Unit

  sys.ShutdownHookThread(shutdown())
}

class RemoteServerActor extends Actor with ActorLogging {
  def receive = {
    case x =>
      log.info("{}", x)
      sender ! x
  }
}
