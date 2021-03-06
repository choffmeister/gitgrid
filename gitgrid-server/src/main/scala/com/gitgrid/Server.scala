package com.gitgrid

import akka.actor._
import akka.io.IO
import com.gitgrid.http.HttpServiceActor
import com.gitgrid.models.Database
import spray.can.Http

import scala.concurrent.duration._

class Server extends Bootable {
  implicit val system = ActorSystem("gitgrid")
  implicit val executor = system.dispatcher
  val coreConf = CoreConfig.load()
  val httpConf = HttpConfig.load()
  val db = Database.open(coreConf.mongoDbServers, coreConf.mongoDbDatabaseName)

  def startup() = {
    val httpServiceActor = system.actorOf(Props(new HttpServiceActor(coreConf, httpConf, db)), "httpservice")

    IO(Http) ! Http.Bind(httpServiceActor, interface = httpConf.interface, port = httpConf.port)
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
