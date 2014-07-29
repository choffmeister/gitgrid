package com.gitgrid.http

import akka.actor._
import com.gitgrid.Config
import spray.routing._

class StaticContentHttpServiceActor(cfg: Config) extends Actor with HttpService {
  implicit val actorRefFactory = context
  implicit val executor = context.dispatcher
  implicit val timeout = akka.util.Timeout(1000)

  def receive = runRoute(route)
  def route = cfg.webDir.map(_.toString) match {
    case Some(webDir) =>
      def index = getFromFile(s"${webDir}/index.html")
      def assets = getFromDirectory(webDir)
      pathSingleSlash(index) ~
      path("index.html")(index) ~
      assets ~
      pathPrefixTest(!("assets" | "scripts" | "styles" | "views"))(index)
    case _ =>
      reject()
  }
}
