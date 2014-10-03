package com.gitgrid.http

import akka.actor._
import akka.io.Tcp._
import com.gitgrid._
import com.gitgrid.models.Database
import spray.http.HttpMethods._
import spray.http.StatusCodes._
import spray.http._

class HttpServiceActor(coreConf: CoreConfig, httpConf: HttpConfig, db: Database) extends Actor with ActorLogging {
  val apiHttpActor = context.actorOf(Props(new ApiHttpServiceActor(coreConf, httpConf, db)))
  val gitHttpActor = context.actorOf(Props(new GitHttpServiceActor(coreConf, httpConf, db)))
  val staticContentHttpActor = context.actorOf(Props(new StaticContentHttpServiceActor(httpConf)))

  def receive = {
    case Connected(_, _) =>
      sender ! Register(self)
    case req@HttpRequest(_, uri, _, _, _) if uri.path.startsWith(Uri.Path("/api")) =>
      log.debug(s"Received request: $req")
      apiHttpActor.tell(req, sender)
    case req@GitHttpRequest(namespace, name, action, service) =>
      log.debug(s"Received GIT request: $namespace/$name $action $service")
      gitHttpActor.tell(req, sender)
    case req@HttpRequest(GET, _, _, _, _) =>
      staticContentHttpActor.tell(req, sender)
    case req@HttpRequest(_, _, _, _, _) =>
      sender ! HttpResponse(status = BadRequest)
  }
}
