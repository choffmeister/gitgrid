package com.gitgrid

import akka.actor.ActorRef
import akka.pattern.ask
import akka.util.Timeout
import spray.http.HttpHeaders.Authorization
import spray.http.HttpMethods._
import spray.http._

trait RequestUtils extends AsyncUtils {
  def req(req: HttpRequest)(implicit httpActor: ActorRef, timeout: Timeout): HttpResponse = {
    await(httpActor ? req).asInstanceOf[HttpResponse]
  }
  def req(method: HttpMethod, uri: String, user: String = "", pass: String = "")(implicit httpActor: ActorRef, timeout: Timeout): HttpResponse = {
    val headers = (user, pass) match {
      case ("", "") => Nil
      case _ => List(Authorization(BasicHttpCredentials(user, pass)))
    }
    req(HttpRequest(method = method, uri = Uri(uri), headers = headers))
  }

  def reqGit(ownerName: String, projectName: String, serviceName: String, user: String = "", pass: String = "")(implicit httpActor: ActorRef, timeout: Timeout) =
    req(GET, s"/$ownerName/$projectName.git/info/refs?service=$serviceName", user, pass)
  def reqGitRead(ownerName: String, projectName: String, user: String = "", pass: String = "")(implicit httpActor: ActorRef, timeout: Timeout) =
    reqGit(ownerName, projectName, "git-upload-pack", user, pass)
  def reqGitWrite(ownerName: String, projectName: String, user: String = "", pass: String = "")(implicit httpActor: ActorRef, timeout: Timeout) =
    reqGit(ownerName, projectName, "git-receive-pack", user, pass)
}
