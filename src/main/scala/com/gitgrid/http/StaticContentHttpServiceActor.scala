package com.gitgrid.http

import akka.actor._
import spray.http._
import spray.routing._

class StaticContentHttpServiceActor extends Actor with HttpService {
  implicit val actorRefFactory = context
  implicit val executor = context.dispatcher
  implicit val timeout = akka.util.Timeout(1000)

  def receive = runRoute(route)
  def route = path(Rest) {
    case filePath if filePath.length > 0 => getFromResource("web/" + filePath)
    case _ => getFromResource("web/index.html")
  }
}
