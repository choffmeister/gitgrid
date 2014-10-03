package com.gitgrid.workers

import akka.actor._

import scala.util._
import scala.collection.mutable.{ Queue => MutableQueue, Map => MutableMap }

import scala.concurrent._
import WorkerProtocol._

object WorkerProtocol {
  case object NoWorkAvailable
  case object WorkAvailable

  case object AskForWork
  case class WorkDone(result: Any)
  case class WorkFailed(error: Throwable)
  case object Register
  case object Unregister

  case class Work(item: Any)
  case class WorkResult(item: Any, result: Any)
}

class WorkerMaster extends Actor with ActorLogging {
  private val queue = MutableQueue.empty[(Work, ActorRef)]
  private val slaves = MutableMap.empty[ActorRef, Option[(Work, ActorRef)]]

  def receive = {
    case Register =>
      val slave = sender()
      if (!slaves.contains(slave)) {
        slaves += slave -> None
        context.watch(slave)
      }

    case Unregister =>
      val slave = sender()
      if (slaves.contains(slave)) {
        if (slaves(slave).nonEmpty) {
          val (work, requester) = slaves(slave).get
          queue.enqueue(work -> requester)
        }
        slaves -= slave
        context.unwatch(slave)
      }

    case Terminated(slave) =>
      if (slaves.contains(slave)) {
        if (slaves(slave).nonEmpty) {
          val (work, requester) = slaves(slave).get
          queue.enqueue(work -> requester)
        }
        slaves -= slave
        context.unwatch(slave)
      }

    case AskForWork =>
      val slave = sender()
      if (queue.nonEmpty && slaves.contains(slave) && slaves(slave).isEmpty) {
        val (work, requester) = queue.dequeue()
        slaves += slave -> Some(work -> requester)
        slave ! work
      } else slave ! NoWorkAvailable

    case WorkDone(result) =>
      val slave = sender()
      if (slaves.contains(slave)) {
        val (work, requester) = slaves(slave).get
        slaves += slave -> None
        requester ! WorkResult(work.item, result)
      }

    case WorkFailed(result) =>
      val slave = sender()
      if (slaves.contains(slave) && slaves(slave).nonEmpty) {
        val (work, requester) = slaves(slave).get
        slaves += slave -> None
        queue.enqueue(work -> requester)
      }

    case w: Work =>
      queue.enqueue(w -> sender())
      slaves.filter(_._2.isEmpty).foreach(_._1 ! WorkAvailable)
  }
}

class WorkerSlave(master: ActorSelection, body: Any => Future[Any]) extends Actor with ActorLogging {
  import ExecutionContext.Implicits.global

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
