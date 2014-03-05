package com.gitgrid

import akka.actor._
import akka.routing._
import spray.http._
import spray.http.HttpMethods._
import spray.http.StatusCodes._

class HttpServiceActor extends Actor with ActorLogging {
  def receive = {
    case akka.io.Tcp.Connected(_, _) =>
      sender ! akka.io.Tcp.Register(self)
    case req@HttpRequest(_, _, _, _, _) =>
      log.debug(s"Received request: ${req}")
      sender ! HttpResponse(entity = s"${req.method} ${req.uri}")
    case o =>
      log.debug(s"Unknown message received: ${o}")
      sender ! HttpResponse(status = NotFound)
  }
}
