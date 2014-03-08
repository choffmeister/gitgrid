package com.gitgrid

import akka.actor._
import akka.io.IO
import scala.concurrent.duration._
import spray.can.Http
import com.gitgrid.http.HttpServiceActor

class Application extends Bootable {
  implicit val system = ActorSystem("gitgrid")
  implicit val config = Config()

  def startup() = {
    val httpServiceActor = system.actorOf(Props(new HttpServiceActor), "httpservice")

    IO(Http) ! Http.Bind(httpServiceActor, interface = config.httpInterface, port = config.httpPort)
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
