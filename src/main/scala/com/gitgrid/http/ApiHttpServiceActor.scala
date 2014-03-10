package com.gitgrid.http

import akka.actor._
import com.gitgrid.Config
import com.gitgrid.auth.AuthenticationHandler
import com.gitgrid.models.User
import spray.routing.HttpService

case class AuthenticationRequest(userName: String, password: String)
case class AuthenticationResponse(message: String, user: Option[User])

class ApiHttpServiceActor(implicit config: Config) extends Actor with ActorLogging with HttpService {
  import JsonProtocol._

  implicit def actorRefFactory = context
  implicit def executor = context.dispatcher
  val authenticationHandler = new AuthenticationHandler()

  def receive = runRoute(route)
  lazy val route =
    pathPrefix("api") {
      pathPrefix("auth") {
        authRoute
      } ~
      path("ping") {
        post {
          complete("pong")
        }
      }
    }

  lazy val authRoute =
    path("login") {
      post {
        entity(as[AuthenticationRequest]) { credentials =>
          onSuccess(authenticationHandler.authenticate(credentials.userName, credentials.password)) {
            case Some(user) => complete(AuthenticationResponse("Logged in", Some(user)))
            case _ => complete(AuthenticationResponse("Invalid username or password", None))
          }
        }
      }
    } ~
    path("logout") {
      post {
        complete("Logout")
      }
    }
}
