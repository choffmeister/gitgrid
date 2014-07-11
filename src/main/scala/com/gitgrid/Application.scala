package com.gitgrid

import akka.actor._
import akka.io.IO
import com.gitgrid.http.HttpServiceActor
import com.gitgrid.models.Database
import scala.concurrent.duration._
import spray.can.Http

class Application extends Bootable {
  implicit val system = ActorSystem("gitgrid")
  implicit val executor = system.dispatcher
  val cfg = Config.load()
  val db = Database.open(cfg.mongoDbServers, cfg.mongoDbDatabaseName)

  def startup() = {
    val httpServiceActor = system.actorOf(Props(new HttpServiceActor(cfg, db)), "httpservice")

    IO(Http) ! Http.Bind(httpServiceActor, interface = cfg.httpInterface, port = cfg.httpPort)
  }

  def shutdown() = {
    system.shutdown()
    system.awaitTermination(1.seconds)
  }
}

object Application {
  def main(args: Array[String]) {
    val app = new Application()
    app.startup()
  }
}

trait Bootable {
  def startup(): Unit
  def shutdown(): Unit

  sys.ShutdownHookThread(shutdown())
}
