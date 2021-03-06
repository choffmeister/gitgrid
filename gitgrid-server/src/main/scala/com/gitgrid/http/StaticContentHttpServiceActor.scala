package com.gitgrid.http

import akka.actor._
import com.gitgrid._
import spray.routing._

class StaticContentHttpServiceActor(httpConf: HttpConfig) extends Actor with HttpService {
  implicit val actorRefFactory = context
  implicit val executor = context.dispatcher
  implicit val timeout = akka.util.Timeout(1000)

  def receive = runRoute(route)
  def route = httpConf.webDir.map(_.toString) match {
    case Some(webDir) =>
      val index = getFromFile(s"${webDir}/index.html")
      val cache = getFromFile(s"${webDir}/cache.manifest")
      val app = getFromDirectory(webDir)
      pathSingleSlash(index) ~
      path("index.html")(index) ~
      path("cache.manifest")(cache) ~
      pathPrefixTest(("app" ~ Slash))(app) ~
      pathPrefixTest(!("app" ~ Slash))(index)
    case _ =>
      reject()
  }
}
