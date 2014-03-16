package com.gitgrid.http

import akka.actor._
import akka.io.Tcp._
import com.gitgrid.git._
import com.gitgrid.models.Database
import spray.http.StatusCodes._
import spray.http._

class HttpServiceActor(db: Database) extends Actor with ActorLogging {
  val apiHttpActor = context.actorOf(Props(new ApiHttpServiceActor(db)))
  val gitHttpActor = context.actorOf(Props(new GitHttpServiceActor(db)))

  def receive = {
    case Connected(_, _) =>
      sender ! Register(self)
    case req@HttpRequest(_, uri, _, _, _) if uri.path.startsWith(Uri.Path("/api")) =>
      log.debug(s"Received request: $req")
      apiHttpActor.tell(req, sender)
    case req@GitHttpRequest(namespace, name, action, service) =>
      log.debug(s"Received GIT request: $namespace/$name $action $service")
      gitHttpActor.tell(req, sender)
    case req: HttpRequest =>
      log.debug(s"Received unknown request: $req")
      sender ! HttpResponse(status = NotFound)
  }
}
