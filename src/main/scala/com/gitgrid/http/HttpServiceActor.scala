package com.gitgrid.http

import akka.actor._
import akka.io.Tcp._
import spray.http.StatusCodes._
import spray.http._
import com.gitgrid.Config

class HttpServiceActor(implicit config: Config) extends Actor with ActorLogging {
  val apiHttpActor = context.actorOf(Props(new ApiHttpServiceActor))

  def receive = {
    case Connected(_, _) =>
      sender ! Register(self)
    case req@HttpRequest(_, uri, _, _, _) if uri.path.startsWith(Uri.Path("/api")) =>
      log.debug(s"Received request: $req")
      apiHttpActor.tell(req, sender)
    case req: HttpRequest =>
      log.debug(s"Received unknown request: $req")
      sender ! HttpResponse(status = NotFound)
  }
}
