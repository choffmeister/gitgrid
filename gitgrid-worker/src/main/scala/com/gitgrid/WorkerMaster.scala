package com.gitgrid

import akka.actor._
import com.gitgrid.WorkerProtocol._

import scala.collection.mutable.{Map => MutableMap, Queue => MutableQueue}

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

  case object Query
  case class QueryResult(queued: List[Any], running: List[Any])
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
          self.tell(work, requester)
        }
        slaves -= slave
        context.unwatch(slave)
      }

    case Terminated(slave) =>
      if (slaves.contains(slave)) {
        if (slaves(slave).nonEmpty) {
          val (work, requester) = slaves(slave).get
          self.tell(work, requester)
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
        self.tell(work, requester)
      }

    case Query =>
      sender ! QueryResult(queue.map(_._1.item).toList, slaves.filter(_._2.nonEmpty).map(_._2.get._1.item).toList)

    case w: Work =>
      queue.enqueue(w -> sender())
      slaves.filter(_._2.isEmpty).foreach(_._1 ! WorkAvailable)
  }
}
