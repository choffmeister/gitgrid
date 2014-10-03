package com.gitgrid

import akka.actor._
import com.gitgrid.WorkerProtocol._

import scala.concurrent._
import scala.util._

class WorkerSlave(master: ActorSelection, body: Any => Future[Any]) extends Actor with ActorLogging {
  import scala.concurrent.ExecutionContext.Implicits.global

  master ! Register
  master ! AskForWork

  def receive = waiting

  def idle: Receive = {
    case WorkAvailable =>
      master ! AskForWork
      context.become(waiting)
  }

  def waiting: Receive = {
    case NoWorkAvailable =>
      context.become(idle)

    case Work(item) =>
      context.become(busy(item))
      body(item).onComplete {
        case Success(res) =>
          master ! WorkDone(res)
          master ! AskForWork
          context.become(waiting)
        case Failure(err) =>
          master ! WorkFailed(err)
          master ! AskForWork
          context.become(waiting)
      }
  }

  def busy(item: Any) = PartialFunction.empty[Any, Unit]
}
