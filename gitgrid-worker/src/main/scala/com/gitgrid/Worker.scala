package com.gitgrid

import akka.actor._
import com.gitgrid.models.Database

import scala.concurrent.duration._

class Worker extends Bootable {
  implicit val system = ActorSystem("gitgrid")
  implicit val executor = system.dispatcher
  val coreConf = CoreConfig.load()
  val db = Database.open(coreConf.mongoDbServers, coreConf.mongoDbDatabaseName)

  def startup() = {
    println("Hello World")
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
