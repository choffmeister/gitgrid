package com.gitgrid.http

import akka.actor._
import com.gitgrid.Config
import spray.http._
import spray.routing._

class StaticContentHttpServiceActor(cfg: Config) extends Actor with HttpService {
  implicit val actorRefFactory = context
  implicit val executor = context.dispatcher
  implicit val timeout = akka.util.Timeout(1000)

  def receive = runRoute(route)
  def route = cfg.webDir.map(_.toString) match {
    case Some(webDir) =>
      val idx = getFromFile(s"${webDir}/index.html")
      pathSingleSlash(idx) ~
      path("login")(idx) ~
      path("logout")(idx) ~
      path("register")(idx) ~
      path("about")(idx) ~
      getFromDirectory(webDir)
    case _ =>
      reject()
  }
}
