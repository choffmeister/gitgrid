package com.gitgrid.http.routes

import java.util.concurrent.TimeUnit

import akka.actor.ActorRefFactory
import akka.pattern.ask
import akka.util.Timeout
import com.gitgrid.WorkerProtocol._
import com.gitgrid._
import com.gitgrid.http.JsonProtocol
import com.gitgrid.models.Database

import scala.concurrent._
import scala.concurrent.duration.FiniteDuration

class WorkersRoutes(val coreConf: CoreConfig, val httpConf: HttpConfig, val db: Database, val actorRefFactory: ActorRefFactory)(implicit val executor: ExecutionContext) extends Routes with JsonProtocol {
  implicit val timeout = Timeout(FiniteDuration(3, TimeUnit.SECONDS))
  val workerMaster = actorRefFactory.actorSelection("/user/worker-master").resolveOne()

  def route = pathEnd((onSuccess(workerMaster.flatMap(ref => ref ? Query)) { case q: QueryResult => complete(q) }))
}
