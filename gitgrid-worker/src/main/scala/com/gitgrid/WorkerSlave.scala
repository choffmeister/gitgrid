package com.gitgrid

import java.util.concurrent.TimeUnit

import akka.actor._
import com.gitgrid.WorkerProtocol._

import scala.concurrent._
import scala.concurrent.duration.FiniteDuration
import scala.util._

class WorkerSlave(master: ActorSelection, body: Any => Future[Any]) extends Actor with ActorLogging {
  import scala.concurrent.ExecutionContext.Implicits.global

  master.resolveOne(FiniteDuration(10, TimeUnit.SECONDS)).onComplete {
    case Success(ref) =>
      context.watch(ref)
      self ! Init
    case Failure(err) =>
      restart("Could not connect to remote actor", 10)
  }

  def receive = uninitialized

  def uninitialized: Receive = {
    case Init =>
      master ! Register
      master ! AskForWork
      context.become(waiting)

    case Terminated(_) =>
      restart("Remote actor terminated", 10)
  }

  def idle: Receive = {
    case WorkAvailable =>
      master ! AskForWork
      context.become(waiting)

    case Terminated(_) =>
      restart("Remote actor terminated", 10)
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

    case Terminated(_) =>
      restart("Remote actor terminated", 10)
  }

  def busy(item: Any): Receive = {
    case Terminated(_) =>
      restart("Remote actor terminated", 10)
  }

  private case object Init
  private def restart(reason: String, seconds: Int): Unit = {
    log.warning(s"$reason. Restarting in 10 seconds...")
    context.become(uninitialized)
    context.system.scheduler.scheduleOnce(FiniteDuration(seconds, TimeUnit.SECONDS), self, Kill)
  }
}
